# spring-demo

1. This demo shows a simple REST service that returns a hello message.
   To troubleshoot the application start it with Flight Recorder.

   java -XX:StartFlightRecording -jar target\hello.app-1.0.0.jar

   Wait for the server to come online.

   To build the hello application, do:
   
   $ mvn package 
   
   Maven uses JAVA_HOME to locate the JVM, so make sure
   it points to JDK 13, or earlier.

2. Open a web brower to http://localhost/hello
   It will display some JSON with an id that is
   increased with every reload.

   Also open the following addresses:

   http://localhost/hello1 and notice it is slower than http://localhost/hello
   http://localhost/hello2 and notice it is slower than http://localhost/hello
   http://localhost/hello3 and notice it is slower than http://localhost/hello


3. Instrumentation has been added to better understand why and when a request
   is slow. In the TraceHandler.java, a JFR event (HttpRequest) is defined and 
   created when a new request comes in. The event is commited to JFR when the
   requests end. In each event, the request URI is stored.

4. Start a second shell where JDK_14\bin is on the path. To dump a recording file, 
   the jcmd tool can be used:

   $ jcmd hello.app.Application JFR.dump filename=dump.jfr

   The recording can be opened in JDK Mission Control, but it is also possible to
   print the contents on command line, the 'jfr' tool'.

   $ jfr print dump.jfr

   It is possible to filter out the HttpRequest event using:
   
   $ jfr print --events HttpRequest dump.jfr
   
   but it will ruin the surprise later.

5. Event Streaming allows events to be accessed without creating a dump.
   This means JFR can be used for monitoring purposes. Monioring can happen in 
   a separate process to reduce impact on the application.

   $ java Monitor.java

6. Switch to the web browser and reload http://localhost/hello a few times.
   The HttpRequest event should now be printed on standard out.

   Monitor.java contains code that checks if a request takes more than 500 ms
   and if so, it checks what other event happened at the same time.

   - Open http://localhost/hello1
     It should print the stack trace of the methods that was running
     at the same time as the request

   - Open http://localhost/hello2 (may need to reload to trigger GC)
     It should print the GCs pauses that happened when the request was running

   - Open http://localhost/hello3
     It should now print which locks the request was waiting for during the request

7. The code to do this is very simple.

   Monitor.java will use the Attach API to see which JVMs are runing on the machine
   with Flight Recorder enabled. The location of the disk repository, which is where JFR
   stores event, is exposed in system property. A stream is opened against the
   directory and if a HttpRequest event is detected that takes more than 500 ms,
   a second stream is started from 1 second before the request to 1 second after.
   The stream will detect if there is lock contention, gc pauses or if a hot
   method was executed at the same time.
