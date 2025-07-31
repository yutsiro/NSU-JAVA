//package factory;
//import factory.model.*;
//
//public class CarStorage extends Storage<Car> {
//    private final FactoryController controller;
//
//    public CarStorage(int capacity, FactoryController controller) {
//        super(capacity);
//        this.controller = controller;
//    }
//
//    @Override
//    public synchronized Car get() throws InterruptedException {
//        while (items.isEmpty()) {
//            wait();
//        }
//        Car item = items.poll();
//        totalProduced.incrementAndGet();
//        controller.onCarRemoved();
//        notifyAll();
//        return item;
//
//    }
//}
