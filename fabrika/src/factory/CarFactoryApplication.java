package factory;

import factory.model.*;
import factory.ui.FactoryUI;
import threadpool.ThreadPool;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class CarFactoryApplication {
    private final Storage<Body> bodyStorage;
    private final Storage<Motor> motorStorage;
    private final Storage<Accessory> accessoryStorage;
    private final Storage<Car> carStorage;
    private final ThreadPool workerPool;
    private final FactoryController controller;
    private final FactoryUI ui;
    private final AtomicBoolean running;//потокобезопасен
    private final PrintWriter logWriter;
    private final List<Shipper<?>> shippers;//обобщенный тип
    private final List<Dealer> dealers;

    public CarFactoryApplication(Properties config) throws IOException {
        int bodyStorageSize = Integer.parseInt(config.getProperty("StorageBodySize", "100"));
        int motorStorageSize = Integer.parseInt(config.getProperty("StorageMotorSize", "100"));
        int accessoryStorageSize = Integer.parseInt(config.getProperty("StorageAccessorySize", "100"));
        int carStorageSize = Integer.parseInt(config.getProperty("StorageAutoSize", "100"));
        int accessoryShippers = Integer.parseInt(config.getProperty("AccessorySuppliers", "5"));
        int workers = Integer.parseInt(config.getProperty("Workers", "10"));
        int dealersCount = Integer.parseInt(config.getProperty("Dealers", "20"));
        boolean logSale = Boolean.parseBoolean(config.getProperty("LogSale", "true"));
        //независимвые объекты
        running = new AtomicBoolean(true);
        logWriter = logSale ? new PrintWriter(new FileWriter("factory_log.txt", false), true) : null;
        shippers = new ArrayList<>();
        dealers = new ArrayList<>();

        bodyStorage = new Storage<>(bodyStorageSize);
        motorStorage = new Storage<>(motorStorageSize);
        accessoryStorage = new Storage<>(accessoryStorageSize);
        carStorage = new Storage<>(carStorageSize);

        controller = new FactoryController(carStorage, bodyStorage, motorStorage, accessoryStorage);
        //carStorage = new CarStorage(carStorageSize, controller);
        //controller.setCarStorage(carStorage);

        workerPool = new ThreadPool(workers, carStorage);
        controller.setWorkerPool(workerPool);

        Shipper<Body> bodyShipper = new Shipper<>("BodyShipper", bodyStorage, ItemType.BODY, 2000);
        Shipper<Motor> motorShipper = new Shipper<>("MotorShipper", motorStorage, ItemType.MOTOR, 2000);
        shippers.add(bodyShipper);
        shippers.add(motorShipper);
        bodyShipper.start();
        motorShipper.start();

        for (int i = 0; i < accessoryShippers; i++) {
            Shipper<Accessory> accessoryShipper = new Shipper<>("AccessoryShipper-" + i, accessoryStorage, ItemType.ACCESSORY, 2000);
            shippers.add(accessoryShipper);
            accessoryShipper.start();
        }

        for (int i = 0; i < dealersCount; i++) {
            Dealer dealer = new Dealer("Dealer-" + i, carStorage, logWriter, 3000);
            dealers.add(dealer);
            dealer.start();
        }

        new Thread(() -> {
            while (running.get()) {
                synchronized (carStorage.getLock()) {
                    int total = carStorage.getCurrentSize() + workerPool.getPendingTasks() + workerPool.getActiveTasks();
                    if (total < carStorage.getCapacity()) {
                        workerPool.submit(new CarAssemblyTask(bodyStorage, motorStorage, accessoryStorage, carStorage));
                        //System.out.println("Submitted new task, total: " + total + ", produced: " + carStorage.getTotalProduced());
                    } else {
                        //System.out.println("Waiting for space, total: " + total + ", produced: " + carStorage.getTotalProduced());
                        try {
                            carStorage.getLock().wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        System.out.println("Resumed task submission");
                    }
                }
            }
        }).start();

        //подаем началььные задачи на старте
        for (int i = 0; i < workers; i++) {
            workerPool.submit(new CarAssemblyTask(bodyStorage, motorStorage, accessoryStorage, carStorage));
        }

        ui = new FactoryUI(this, bodyStorage, motorStorage, accessoryStorage, carStorage, workerPool, shippers, dealers);
    }

    public void stop() {
        running.set(false);
        workerPool.shutdown();
        shippers.forEach(Thread::interrupt);
        dealers.forEach(Thread::interrupt);
        if (logWriter != null) {
            logWriter.close();
        }
    }

    public static void main(String[] args) {
        try {
            Properties config = new Properties();
            try (InputStream input = new FileInputStream("factory_config.properties")) {
                config.load(input);
            }
            new CarFactoryApplication(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
