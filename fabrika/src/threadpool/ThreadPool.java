package threadpool;

import factory.Storage;
import factory.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool {
    private final BlockingQueue<Runnable> tasks;//очередь для хранения задач
    private final List<Thread> workers;//пул потоков исполнителей
    private volatile boolean isRunning;//используется для завершения потоков
    private final Storage<Car> carStorage; // ссылка на склад машин
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    public ThreadPool(int poolSize, Storage<Car> carStorage) {
        tasks = new LinkedBlockingQueue<>();
        workers = new ArrayList<>();
        isRunning = true;
        this.carStorage = carStorage;

        for (int i = 0; i < poolSize; i++) {
            Thread worker = new Thread(() -> {
                while (isRunning) {//пока пул активен
                    try {
                        Runnable task = tasks.take();//блокируется если очередь пуста
                        activeTasks.incrementAndGet();
                        //System.out.println("ThreadPool: Starting task, active tasks: " + activeTasks.get());
                        task.run();
                        activeTasks.decrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "Worker-" + i);
            worker.start();
            workers.add(worker);
        }
    }

    public void submit(Runnable task) {
        if (isRunning && carStorage.getCurrentSize() + tasks.size() +
                activeTasks.get() < carStorage.getCapacity()) {
            tasks.offer(task);
        } else {
            System.out.println("Cannot submit task: storage or queue would exceed capacity");
        }
    }

    public void shutdown() {//прерывает все рабочие потоки
        isRunning = false;
        for (Thread worker : workers) {
            worker.interrupt();
        }
    }

    public int getPendingTasks() {
        return tasks.size();
    }

    public int getActiveTasks() {
        return activeTasks.get();
    }
}