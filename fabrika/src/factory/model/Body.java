package factory.model;

import java.util.concurrent.atomic.AtomicInteger;

public class Body extends Element {
    private static final AtomicInteger nextId = new AtomicInteger(0);

    public Body() {
        super(nextId.getAndIncrement());
    }
}