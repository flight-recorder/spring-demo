import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

public final class Monitor {
	private final static Map<String, EventStream> monitored = new HashMap<>();
	private final static String REPOSITORY = "jdk.jfr.repository";

	public static void main(String... args) throws InterruptedException {
		System.out.println("JFR Event streaming client started.");
		while (true) {
			for (VirtualMachineDescriptor d : VirtualMachine.list()) {
				try {
					tryMonitor(d);
				} catch (Exception e) {
					// ignore
				}
			}
			Thread.sleep(1000);
		}
	}
	
	private static void tryMonitor(VirtualMachineDescriptor d) throws AttachNotSupportedException, IOException {
		VirtualMachine vm = VirtualMachine.attach(d);
		Properties p = vm.getSystemProperties();
		String repo = p.getProperty(REPOSITORY);
		if (repo != null) {
			monitor(d.id(), d.displayName(), repo);
		}
	}

	private static void monitor(String pid, String name, String repository) throws IOException {
		if (monitored.containsKey(pid)) {
			return;
		}
		Path path = Paths.get(repository);
		EventStream es = EventStream.openRepository(path);
		es.onEvent("HttpRequest", e -> {
			String requestURI = e.getString("requestURI");
			System.out.println("Pid: " + pid + " HTTP Request: " + requestURI + " duration=" + e.getDuration().toMillis() + " ms");
			if (e.getDuration().toMillis() > 500) {
				analyze(pid, e, path);
			}
		});
		monitored.put(pid, es);
		System.out.println("Monitoring: pid=" + pid + " name=" + name);
		es.startAsync();
	}

	private static void analyze(String pid, RecordedEvent request, Path path) {
		System.out.print("Slow request detected. Analyzing ... ");
		long threadId = request.getThread().getId();
		try (EventStream es = EventStream.openRepository(path)) {
			es.setStartTime(request.getStartTime().minus(Duration.ofSeconds(1)));
			es.setEndTime(request.getEndTime().plus(Duration.ofSeconds(1)));
			es.onEvent("jdk.GCPhasePause", event -> {
				if (overlap(event, request) && event.getDuration().toMillis() > 50) {
					System.out.println("GC pause ");
					System.out.println(event);
				}
			});
			es.onEvent("jdk.JavaMonitorWait", event -> {
				if (event.getThread().getId() == threadId) {
					if (overlap(event, request)) {
						System.out.println("Lock contention ");
						System.out.println(event);
					}
				}
			});
			AtomicLong counter = new AtomicLong();
			es.onEvent("jdk.ExecutionSample", event -> {
				RecordedThread s = event.getThread("sampledThread");
				if (s != null && s.getId() == threadId) {
					if (overlap(event, request)) {
						counter.getAndIncrement();
						if (counter.get() == 5) {
							System.out.println("Hot method ");
							System.out.println(event);
						}
					}
				}
			});
			es.startAsync();
			// time out after 3 seconds
			Thread.sleep(3_000); 
		} catch (Exception ioe) {
			// ignore
		}
	}

	private static boolean overlap(RecordedEvent event, RecordedEvent request) {
		Instant rStart = request.getStartTime();
		Instant rEnd = request.getEndTime();
		Instant eStart = event.getStartTime();
		Instant eEnd = event.getEndTime();
		if (eStart.isAfter(rStart) && eStart.isBefore(rEnd)) {
			return true;
		}
		if (eEnd.isAfter(rStart) && eEnd.isBefore(rEnd)) {
			return true;
		}
		return false;
	}
}
