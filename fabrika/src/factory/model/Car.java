package factory.model;

import java.util.concurrent.atomic.AtomicInteger;

public class Car extends Element {
    private static final AtomicInteger nextID = new AtomicInteger(0);
    //private final long id;
    private final Body body;
    private final Motor motor;
    private final Accessory accessory;

    public Car(Body body, Motor motor, Accessory accessory) {
        super(nextID.getAndIncrement());
        this.body = body;
        this.motor = motor;
        this.accessory = accessory;
    }

    public Body getBody() {
        return body;
    }

    public Motor getMotor() {
        return motor;
    }

    public Accessory getAccessory() {
        return accessory;
    }
}