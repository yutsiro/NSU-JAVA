import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class DHCPServer {
    private DatagramSocket socket;//DHCP работает через UDP
    // (порт 67 для сервера, 68 для клиента)
    private Map<String, String> leasedIPs; // IP -> MAC
    private List<String> availableIPs; // пул свободных IP
    private ExecutorService executor;

    public DHCPServer(int port) throws IOException {
        socket = new DatagramSocket(port);//сокет на указанном порту
        leasedIPs = new ConcurrentHashMap<>();
        availableIPs = new CopyOnWriteArrayList<>();
        executor = Executors.newCachedThreadPool();
        // заполняем пул IP-адресов
        for (int i = 2; i <= 254; i++) {
            availableIPs.add("192.168.1." + i);
        }
        System.out.println("DHCP-сервер запущен на порту " + port);
    }

    public void start() {
        try {
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);//блокирует выполнение кста пока пакет не придет
                executor.execute(() -> handleRequest(packet));//каждый пакет в своем потоке
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void handleRequest(DatagramPacket packet) {
        try {
            String message = new String(packet.getData(), 0, packet.getLength());
            String[] parts = message.split(" ");
            if (parts[0].equals("DHCPDISCOVER")) {
                String clientMac = parts[1];
                String ip = assignIP(clientMac);
                if (ip != null) {
                    // отправляем DHCPOFFER
                    String response = "DHCPOFFER " + ip;
                    sendResponse(response, packet.getAddress(), packet.getPort());
                    System.out.println("Предложен IP: " + ip + " для MAC: " + clientMac);
                }
            } else if (parts[0].equals("DHCPREQUEST")) {
                String clientMac = parts[1];
                String requestedIP = parts[2];
                if (leasedIPs.get(requestedIP) != null && leasedIPs.get(requestedIP).equals(clientMac)) {
                    // подтверждаем выделение айпи
                    String response = "DHCPACK " + requestedIP;
                    sendResponse(response, packet.getAddress(), packet.getPort());
                    System.out.println("Подтверждён IP: " + requestedIP + " для MAC: " + clientMac);
                } else {
                    // отклоняем запрос
                    String response = "DHCPNAK";
                    sendResponse(response, packet.getAddress(), packet.getPort());
                }
            } else if (parts[0].equals("DHCPRELEASE")) {
                String clientMac = parts[1];
                String releasedIP = parts[2];
                synchronized (leasedIPs) {
                    if (leasedIPs.get(releasedIP) != null && leasedIPs.get(releasedIP).equals(clientMac)) {
                        leasedIPs.remove(releasedIP);
                        synchronized (availableIPs) {
                            availableIPs.add(releasedIP);
                        }
                        System.out.println("IP " + releasedIP + " освобождён клиентом " + clientMac);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка обработки запроса: " + e.getMessage());
        }
    }
    //один поток может выделять айпи одновременно
    private synchronized String assignIP(String mac) {
        //проверяем, не занят ли уже IP для этого MAC
        for (Map.Entry<String, String> entry : leasedIPs.entrySet()) {
            if (entry.getValue().equals(mac)) {
                return entry.getKey();
            }
        }
        //выделяем новый IP из пула
        if (!availableIPs.isEmpty()) {
            String ip = availableIPs.remove(0);
            leasedIPs.put(ip, mac);
            return ip;
        }
        return null; //пул исчерпан
    }

    private void sendResponse(String response, InetAddress address, int port) throws IOException {
        byte[] buffer = response.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packet);
    }

    public void stop() {
        socket.close();
        executor.shutdown();
        leasedIPs.clear();
        availableIPs.clear();
    }

    public static void main(String[] args) throws IOException {
        DHCPServer server = new DHCPServer(67); // порт DHCP по стандарту
        server.start();
    }
}