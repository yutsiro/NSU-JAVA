import java.io.*;
import java.net.*;

public class ExternalNode {
    private String ip;
    private String mac;
    private String routerHost;
    private int routerPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running;

    public ExternalNode(String ip, String mac, String routerHost, int routerPort) {
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
            System.out.println("Внешний узел " + ip + "/" + mac + " подключён к маршрутизатору");

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
                if (message.startsWith("PING от")) {
                    System.out.println("Получено: " + message);
                    String sourceIp = message.split(" ")[2].split("/")[0];
                    out.println("PING-REPLY " + sourceIp + " " + ip);
                    System.out.println("Отправлен PING-REPLY на " + sourceIp);
                } else if (message.startsWith("ERROR")) {
                    System.out.println("Получено: " + message);
                    stop();
                    System.exit(1);
                }
            }
        } catch (IOException e) {
            System.err.println("Отключён от маршрутизатора: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        System.out.println("Внешний узел останавливается...");
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
            System.out.println("Внешний узел остановлен");
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии ресурсов: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        ExternalNode node = new ExternalNode("203.0.113.2", "00:1A:2B:3C:4D:03", "localhost", 9999);
        node.connect();

        //просто ждет сообщений
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Нажмите Enter для выхода...");
        console.readLine();
        node.stop();
    }
}