import java.io.*;
import java.net.*;

public class Client2 {
    private String ip;
    private String mac;
    private String routerHost;//параметры маршрутизатора
    private int routerPort;
    private Socket socket;//местный сокет+потоки ввода/вывода
    private BufferedReader in;
    private PrintWriter out;
    private boolean running;
    private final Object arpLock = new Object();//для синхроназицаии ARP-запросов
    private String arpResponse; //хранит MAC из ARP-ответа

    public Client2(String mac, String routerHost, int routerPort) {
        //this.ip = ip;
        this.mac = mac;
        this.routerHost = routerHost;
        this.routerPort = routerPort;
        this.running = true;
    }

    public boolean obtainIP() {
        try (DatagramSocket dhcpSocket = new DatagramSocket()) {
            dhcpSocket.setBroadcast(true);
            // отправляем DHCPDISCOVER
            String message = "DHCPDISCOVER " + mac;
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName("255.255.255.255"), 67);
            dhcpSocket.send(packet);
            System.out.println("Отправлен DHCPDISCOVER");

            // ждем DHCPOFFER
            buffer = new byte[1024];
            packet = new DatagramPacket(buffer, buffer.length);
            dhcpSocket.setSoTimeout(5000);
            dhcpSocket.receive(packet);
            String response = new String(packet.getData(), 0, packet.getLength());
            if (response.startsWith("DHCPOFFER")) {
                ip = response.split(" ")[1];
                System.out.println("Получен IP: " + ip);

                // отправляем DHCPREQUEST
                message = "DHCPREQUEST " + mac + " " + ip;
                buffer = message.getBytes();
                packet = new DatagramPacket(buffer, buffer.length,
                        packet.getAddress(), packet.getPort());
                dhcpSocket.send(packet);

                // ждем DHCPACK
                buffer = new byte[1024];
                packet = new DatagramPacket(buffer, buffer.length);
                dhcpSocket.receive(packet);
                response = new String(packet.getData(), 0, packet.getLength());
                if (response.startsWith("DHCPACK")) {
                    System.out.println("IP " + ip + " подтверждён");
                    return true;
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка получения IP: " + e.getMessage());
        }
        return false;
    }

    //к роутеру
    public void connect() {
        if (!obtainIP()) {
            System.err.println("Не удалось получить IP-адрес");
            stop();
            return;
        }
        try {//инициализация сокета, потоков и/о
            socket = new Socket(routerHost, routerPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //регистрируемся у роутера
            out.println("REGISTER " + ip + " " + mac);
            System.out.println("Клиент " + ip + "/" + mac + " подключён к маршрутизатору");

            //отдельный поток для чтения сообщений от роутера
            Thread receiveThread = new Thread(this::receiveMessages);
            receiveThread.setDaemon(true);
            receiveThread.start();
            //new Thread(this::receiveMessages).start();
            //демон-потоки не мешают завершению программы: завершаются вместе
            //с юзер-потоками
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
                    synchronized (arpLock) {//получили ответ от роутера - уведомляем поток, который отправлял запрос
                        arpResponse = message.split(" ")[1]; //сохраняем MAC
                        arpLock.notify();
                    }
                } else if (message.startsWith("PING от")) {
                    System.out.println("Получено: " + message);
                    //отправляем пинг-реплай
                    String sourceIp = message.split(" ")[2].split("/")[0];
                    out.println("PING_REPLY " + ip + sourceIp);
                    System.out.println("Отправлен PING_REPLY на " + sourceIp);
                } else if (message.startsWith("PING_REPLY от")) {
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

    //хочим получить мас-адрес по айпи
    public String sendArpRequest(String targetIp) {
        if (!running) return null;
        synchronized (arpLock) {//синхронизируем доступ чтобы только этот поток мог работать с arpResponse
            arpResponse = null;//предыдущий ответ сбрасываем
            System.out.print("Отправляю ARP-запрос для " + targetIp + "... ");
            out.println("ARP-REQUEST " + targetIp);//отправляем запрос
            try {
                arpLock.wait(5000); //ждем ответа 5 сек либо пока другой поток не вызовет arpLock.notify()
            } catch (InterruptedException e) {
                System.err.println("Прервано ожидание ARP-ответа: " + e.getMessage());
            }
            return arpResponse; //возвращаем мас-адрес или null
        }
    }

    //запрашиваем мас-адрес, отправляем запрос по айпи
    public void sendPing(String targetIp) {
        if (!running) return;
        String targetMac = sendArpRequest(targetIp);
        if (targetMac != null) {
            System.out.println("Успешный ARP-запрос!");
            try {
                out.println("PING " + targetIp + " " + ip);
                System.out.println("Отправлен PING на " + targetIp + " (MAC: " + targetMac + ")");
            } catch (Exception e) {
                System.err.println("Ошибка при отправке PING: " + e.getMessage());
                stop();
            }
        } else {
            System.out.println("Не удалось найти MAC для " + targetIp);
        }
    }

    //завершение работы клиента
    public void stop() {
        running = false;
        try {
            if (ip != null) {
                try (DatagramSocket dhcpSocket = new DatagramSocket()) {
                    String message = "DHCPRELEASE " + mac + " " + ip;
                    byte[] buffer = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                            InetAddress.getByName("255.255.255.255"), 67);
                    dhcpSocket.send(packet);
                    System.out.println("Отправлен DHCPRELEASE для IP: " + ip);
                }
            }
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии: " + e.getMessage());
        }
    }

    //создаем клиент с заданными айпи и мас-адресом, коннектимся к роутеру
    public static void main(String[] args) throws IOException {
        Client2 client = new Client2("00:1A:2B:3C:4D:03", "localhost", 9999);
        client.connect();

        //создаем потоок для чтения с консоли
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        while (client.running) {
            System.out.println("Введите IP для PING (или 'exit' для выхода): ");
            try {
                String input = console.readLine();
                if (input == null || "exit".equalsIgnoreCase(input)) {
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