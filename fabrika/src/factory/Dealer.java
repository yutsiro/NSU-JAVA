package factory;

import factory.model.Car;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Dealer extends Thread {
    private final Storage<Car> carStorage;
    private final PrintWriter logWriter;//логи продаж
    private long delay;//задержка М
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    //^ формат времени в логе

    public Dealer(String name, Storage<Car> carStorage, PrintWriter logWriter, long delay) {
        super(name);//передается в конструктор Thread
        this.carStorage = carStorage;
        this.logWriter = logWriter;
        this.delay = delay;
        setDaemon(true);
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                System.out.println(getName() + " requesting car");
                Car car = carStorage.get();
                if (logWriter != null) {
                    synchronized (logWriter) {
                        logWriter.println(String.format("%s: %s: Auto %d (Body: %d, Motor: %d, Accessory: %d)",
                                LocalDateTime.now().format(FORMATTER), getName(), car.getId(),
                                car.getBody().getId(), car.getMotor().getId(), car.getAccessory().getId()));
                    }
                }
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}