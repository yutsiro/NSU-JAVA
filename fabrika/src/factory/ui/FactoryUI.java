package factory.ui;

import factory.*;
import factory.model.*;
import threadpool.ThreadPool;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class FactoryUI {
    private final CarFactoryApplication app;//для вызова stop() при закрытии
    private final Storage<Body> bodyStorage;
    private final Storage<Motor> motorStorage;
    private final Storage<Accessory> accessoryStorage;
    private final Storage<Car> carStorage;
    private final ThreadPool workerPool;
    private final List<Shipper<?>> shippers;
    private final List<Dealer> dealers;
    private final JFrame frame;
    private final JLabel bodyLabel;
    private final JLabel motorLabel;
    private final JLabel accessoryLabel;
    private final JLabel carLabel;
    private final JLabel tasksLabel;
    private final JLabel totalCarsLabel;

    public FactoryUI(CarFactoryApplication app, Storage<Body> bodyStorage, Storage<Motor> motorStorage,
                     Storage<Accessory> accessoryStorage, Storage<Car> carStorage, ThreadPool workerPool,
                     List<Shipper<?>> shippers, List<Dealer> dealers) {
        this.app = app;
        this.bodyStorage = bodyStorage;
        this.motorStorage = motorStorage;
        this.accessoryStorage = accessoryStorage;
        this.carStorage = carStorage;
        this.workerPool = workerPool;
        this.shippers = shippers;
        this.dealers = dealers;

        frame = new JFrame("Car Factory");//основное окно
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        //текстовые элементы для отображения состояния складов
        bodyLabel = new JLabel("Body Storage: 0/" + bodyStorage.getCapacity());
        motorLabel = new JLabel("Motor Storage: 0/" + motorStorage.getCapacity());
        accessoryLabel = new JLabel("Accessory Storage: 0/" + accessoryStorage.getCapacity());
        carLabel = new JLabel("Car Storage: 0/" + carStorage.getCapacity());
        tasksLabel = new JLabel("Pending Tasks: 0");
        totalCarsLabel = new JLabel("Total Cars Produced: 0");

        frame.add(createMainPanel());//основная панель
        frame.addWindowListener(new java.awt.event.WindowAdapter() {//реакция на закрытие окна
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                app.stop();
            }
        });
        frame.setVisible(true);

        startUpdateThread();
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 4));

        //создаем ползунки
        JSlider bodySpeed = new JSlider(150, 20000, 2000);
        JSlider motorSpeed = new JSlider(150, 20000, 2000);
        JSlider accessorySpeed = new JSlider(450, 20000, 2000);
        JSlider dealerSpeed = new JSlider(500, 20000, 5000);

        //каждый слайдер слушает изменения
        bodySpeed.addChangeListener(e -> shippers.stream()
                .filter(s -> s.getName().startsWith("BodyShipper"))
                .forEach(s -> s.setDelay(bodySpeed.getValue())));
        motorSpeed.addChangeListener(e -> shippers.stream()
                .filter(s -> s.getName().startsWith("MotorShipper"))
                .forEach(s -> s.setDelay(motorSpeed.getValue())));
        accessorySpeed.addChangeListener(e -> shippers.stream()
                .filter(s -> s.getName().startsWith("AccessoryShipper"))
                .forEach(s -> s.setDelay(accessorySpeed.getValue())));
        dealerSpeed.addChangeListener(e -> dealers.forEach(d -> d.setDelay(dealerSpeed.getValue())));

        //добавляем на панель метки и слайдеры
        panel.add(new JLabel("Body Storage:"));
        panel.add(bodyLabel);
        panel.add(new JLabel("Motor Storage:"));
        panel.add(motorLabel);
        panel.add(new JLabel("Accessory Storage:"));
        panel.add(accessoryLabel);
        panel.add(new JLabel("Car Storage:"));
        panel.add(carLabel);
        panel.add(new JLabel("Pending Tasks:"));
        panel.add(tasksLabel);
        panel.add(new JLabel("Total Cars Produced:"));
        panel.add(totalCarsLabel);
        panel.add(new JLabel("Body Shipper Speed:"));
        panel.add(bodySpeed);
        panel.add(new JLabel("Motor Shipper Speed:"));
        panel.add(motorSpeed);
        panel.add(new JLabel("Accessory Shipper Speed:"));
        panel.add(accessorySpeed);
        panel.add(new JLabel("Dealer Speed:"));
        panel.add(dealerSpeed);

        return panel;
    }

    private void startUpdateThread() {//отдельный поток для обновления UI каждые 100мс
        new Thread(() -> {
            while (frame.isVisible()) {
                SwingUtilities.invokeLater(() -> {
//                    int bodySize = bodyStorage.getCurrentSize();
//                    System.out.println("Body Storage size: " + bodySize);
                    bodyLabel.setText(bodyStorage.getCurrentSize() + "/" + bodyStorage.getCapacity());
                    motorLabel.setText(motorStorage.getCurrentSize() + "/" + motorStorage.getCapacity());
                    accessoryLabel.setText(accessoryStorage.getCurrentSize() + "/" + accessoryStorage.getCapacity());
                    carLabel.setText(carStorage.getCurrentSize() + "/" + carStorage.getCapacity());
                    tasksLabel.setText(" " + workerPool.getPendingTasks());
                    totalCarsLabel.setText(" " + carStorage.getTotalProduced());
                });
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
}