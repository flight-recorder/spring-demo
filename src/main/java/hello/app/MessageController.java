package hello.app;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class MessageController {

    @RequestMapping("/hello")
    public Message hello() {
        return new Message("hello");
    }

    @RequestMapping("/hello1")
    public Message hello1() {
        cpuIntensive();
        return new Message("hello 1");
    }

    @RequestMapping("/hello2")
    public Message hello2() {
        gcIntensive();
        return new Message("hello 2");
    }

    @RequestMapping("/hello3")
    public Message hello3() {
        lockContention();
        return new Message("hello 3");
    }

    public Object value;

    private void gcIntensive() {
        for (int i = 0; i < 1000; i++) {
            value = new Object[1024 * 1024];
        }
    }

    public long number;

    private void cpuIntensive() {
        for (long l = 0; l < 1000_000_000; l++) {
            number += l;
        }
    }

    public Object lock = new Object();

    private void lockContention() {
        synchronized (lock) {
            try {
                lock.wait(1000);
            } catch (InterruptedException e) {
            }
        }
    }
}
