package factory.model;

import java.util.concurrent.atomic.AtomicInteger;

public class Accessory extends Element {
    private static final AtomicInteger nextId = new AtomicInteger(0);

    public Accessory() {
        super(nextId.getAndIncrement());
    }
}