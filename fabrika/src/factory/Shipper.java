package factory;

import factory.model.*;

public class Shipper<T extends Element> extends Thread {
    private final Storage<T> storage;
    private ItemType itemType;
    private long delay;//задержка N

    public Shipper(String name, Storage<T> storage, ItemType itemType, long delay) {
        super(name);//передается в конструктор Thread
        this.storage = storage;
        this.itemType = itemType;
        this.delay = delay;
        setDaemon(true);//завершается
    }

    public void setDelay(long delay) {//UI для регулировки скорости
        this.delay = delay;
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                System.out.println(getName() + " producing item");
                T item = createItem();
                storage.put(item);
                //System.out.println(getName() + " added item to storage, current size: " + storage.getCurrentSize());
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private T createItem() {
        switch (itemType) {
            case BODY:
                return (T) new Body();
            case MOTOR:
                return (T) new Motor();
            case ACCESSORY:
                return (T) new Accessory();
            default:
                throw new IllegalStateException("Unknown item type: " + itemType);
        }
    }
}

