package factory.model;

import java.util.concurrent.atomic.AtomicInteger;

public class Motor extends Element {
    private static final AtomicInteger nextId = new AtomicInteger(0);

    public Motor() {
        super(nextId.getAndIncrement());
    }
}