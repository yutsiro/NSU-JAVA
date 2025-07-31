package factory;

import factory.model.*;

public class CarAssemblyTask implements Runnable {
    private final Storage<Body> bodyStorage;
    private final Storage<Motor> motorStorage;
    private final Storage<Accessory> accessoryStorage;
    private final Storage<Car> carStorage;

    public CarAssemblyTask(Storage<Body> bodyStorage, Storage<Motor> motorStorage,
                           Storage<Accessory> accessoryStorage, Storage<Car> carStorage) {
        this.bodyStorage = bodyStorage;
        this.motorStorage = motorStorage;
        this.accessoryStorage = accessoryStorage;
        this.carStorage = carStorage;
    }

    @Override
    public void run() {
        try {
            Body body = bodyStorage.get();
            Motor motor = motorStorage.get();
            Accessory accessory = accessoryStorage.get();
            Thread.sleep(500);
            Car car = new Car(body, motor, accessory);
            carStorage.put(car);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}