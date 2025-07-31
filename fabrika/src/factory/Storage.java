package factory;

import factory.model.*;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class Storage<T extends Element> {
    public final Queue<T> items;
    private final int capacity;
    private final AtomicInteger totalProduced = new AtomicInteger(0);
    private final Object lock = new Object();

    public Storage(int capacity) {//для склада автомобилей
        this.capacity = capacity;
        this.items = new ArrayDeque<>(capacity);
    }

    public void put(T item) throws InterruptedException {
        synchronized (lock) {
            while (items.size() >= capacity) {
                lock.wait();
            }
            items.offer(item);
            totalProduced.incrementAndGet();
            //System.out.println("Storage: Added item, current size: " + items.size() + ", total produced: " + totalProduced.get());
            lock.notifyAll();
        }
    }

    public T get() throws InterruptedException {
        synchronized (lock) {
            while (items.isEmpty()) {
                lock.wait();
            }
            T item = items.poll();
            //System.out.println("Storage: Removed item, current size: " + items.size() + ", total produced: " + totalProduced.get());
            if (item instanceof Car) {
                lock.notifyAll(); //уведомляем контроллер о доступности машины
            }
            return item;
        }
    }

    public int getCurrentSize() {
        synchronized (lock) {
            return items.size();
        }
    }

    public synchronized AtomicInteger getTotalProduced() {
        return totalProduced;
    }

    public int getCapacity() {
        return capacity;
    }

    public Object getLock() {
        return lock;
    }
}