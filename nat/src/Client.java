import java.io.*;
import java.net.*;

public class Client {
    private String ip;
    private String mac;
    private String routerHost;
    private int routerPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running;
    private String arpResponse;
    private final Object arpLock = new Object();

    public Client(String ip, String mac, String routerHost, int routerPort) {
        this.ip = ip;
        this.mac = mac;
        this.routerHost = routerHost;
        this.routerPort = routerPort;
        this.running = true;
    }

    public void connect() {
        try {
            socket = new Socket(routerHost, routerPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("REGISTER " + ip + " " + mac);
            System.out.println("Клиент " + ip + "/" + mac + " подключён к маршрутизатору");

            Thread receiveThread = new Thread(this::receiveMessages);
            receiveThread.setDaemon(true);
            receiveThread.start();
        } catch (IOException e) {
            System.err.println("Ошибка подключения: " + e.getMessage());
            running = false;
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                if (message.startsWith("ARP-REPLY")) {
                    System.out.println("Получено: " + message);
                    synchronized (arpLock) {
                        arpResponse = message.split(" ")[1];
                        arpLock.notify();
                    }
                } else if (message.startsWith("PING от")) {
                    System.out.println("Получено: " + message);
                    String sourceIp = message.split(" ")[2].split("/")[0];
                    out.println("PING-REPLY " + sourceIp + " " + ip);
                    System.out.println("Отправлен PING-REPLY на " + sourceIp);
                } else if (message.startsWith("PING-REPLY от")) {
                    System.out.println("Получено: " + message);
                } else {
                    System.out.println("Получено: " + message);
                    if (message.startsWith("ERROR")) {
                        stop();
                        System.exit(1);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Отключён от маршрутизатора: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public String sendArpRequest(String targetIp) {
        if (!running) return null;
        synchronized (arpLock) {
            arpResponse = null;
            System.out.print("Отправляю ARP-запрос для " + targetIp + "... ");
            out.println("ARP-REQUEST " + targetIp);
            System.out.println("Отправлен ARP-запрос для " + targetIp);
            try {
                arpLock.wait(5000);
            } catch (InterruptedException e) {
                System.err.println("Прервано ожидание ARP-ответа: " + e.getMessage());
            }
            return arpResponse;
        }
    }

    public void sendPing(String targetIp) {
        if (!running) return;
        String targetMac = sendArpRequest(targetIp);
        if (targetMac != null) {
            try {
                out.println("PING " + targetIp + " " + ip);
                System.out.println("Отправлен PING на " + targetIp);
            } catch (Exception e) {
                System.err.println("Ошибка при отправке PING: " + e.getMessage());
                stop();
            }
        } else {
            System.out.println("Не удалось найти MAC для " + targetIp);
        }
    }

    public void stop() {
        running = false;
        System.out.println("Клиент останавливается...");
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Сокет закрыт");
            }
            if (out != null) {
                out.close();
                System.out.println("Поток вывода закрыт");
            }
            if (in != null) {
                in.close();
                System.out.println("Поток ввода закрыт");
            }
            System.out.println("Клиент остановлен");
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии ресурсов: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client("192.168.1.2", "00:1A:2B:3C:4D:02", "localhost", 9999);
        client.connect();

        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        while (client.running) {
            System.out.println("Введите IP для PING (или 'exit' для выхода): ");
            try {
                String input = console.readLine();
                System.out.println("Введено: " + input);
                if (input == null || "exit".equalsIgnoreCase(input.trim())) {
                    System.out.println("Выход...");
                    client.stop();
                    console.close();
                    System.exit(0);
                }
                client.sendPing(input);
            } catch (IOException e) {
                System.err.println("Ошибка чтения консоли: " + e.getMessage());
                client.stop();
                System.exit(1);
            }
        }
    }
}