package factory;

import factory.model.*;
import threadpool.ThreadPool;

public class FactoryController {
    private Storage<Car> carStorage;
    private ThreadPool workerPool;//пул потоков для задач сборки
    private final Storage<?> bodyStorage;//склады для передачи Worker
    private final Storage<?> motorStorage;
    private final Storage<?> accessoryStorage;

    public FactoryController(Storage<Car> carStorage,
                             Storage<?> bodyStorage, Storage<?> motorStorage,
                             Storage<?> accessoryStorage) {
        this.carStorage = carStorage;
        this.bodyStorage = bodyStorage;
        this.motorStorage = motorStorage;
        this.accessoryStorage = accessoryStorage;
    }

    public void setCarStorage(Storage<Car> carStorage) {
        this.carStorage = carStorage;
    }

    public void setWorkerPool(ThreadPool workerPool) {
        this.workerPool = workerPool;
    }

//    public synchronized void onCarRemoved() {
//        if (carStorage.getCurrentSize() < carStorage.getCapacity()
//                && carStorage.getCurrentSize() + workerPool.getPendingTasks() < carStorage.getCapacity()) {
//            workerPool.submit(new CarAssemblyTask(bodyStorage, motorStorage, accessoryStorage, carStorage));
//        }//добавляет задачу(runnable) в очередь задач
//    }

    public synchronized void onCarRemoved() {
        synchronized (carStorage) {
            while (carStorage.getCurrentSize() + workerPool.getPendingTasks() >= carStorage.getCapacity()) {
                try {
                    carStorage.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            workerPool.submit(new CarAssemblyTask((Storage<Body>) bodyStorage, (Storage<Motor>) motorStorage,
                    (Storage<Accessory>) accessoryStorage, carStorage));
        }
    }
}