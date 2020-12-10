package hello.app;

import java.util.concurrent.atomic.AtomicLong;

public final class Message {
    private static final AtomicLong counter = new AtomicLong();

    private final long id;
    private final String message;

    public Message(String message) {
        this.id = counter.incrementAndGet();
        this.message = message;
    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return message;
    }
}
