import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NodeDiscovery {
    private final String multicastAddress;
    private final int port;
    private final String nodeId; //генерируется уникально
    private final MulticastSocket socket;
    private final InetAddress group;
    private final Map<String, NodeInfo> activeNodes;
    private final long timeoutMs = 10000; //таймаут 10 сек
    private final ScheduledExecutorService scheduler;

    private static class NodeInfo {
        String ipAddress;
        long lastSeen; //timestamp

        NodeInfo(String ipAddress, long lastSeen) {
            this.ipAddress = ipAddress;
            this.lastSeen = lastSeen;
        }
    }

    public NodeDiscovery(String multicastAddress, int port) throws IOException {
        this.multicastAddress = multicastAddress;
        this.port = port;
        this.nodeId = UUID.randomUUID().toString();
        this.activeNodes = new ConcurrentHashMap<>();
        //InetAddress преборазует строку с адресом в объект InetAddress, для ipv4/6 - Inet4/6Address
        this.group = InetAddress.getByName(multicastAddress);
        this.socket = new MulticastSocket(port);//UDP-socket
        //пул из 2 потоков для выполнения периодических задач
        this.scheduler = Executors.newScheduledThreadPool(2);

        socket.joinGroup(new InetSocketAddress(group, port), NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
        System.out.println("Node started with ID: " + nodeId + " on multicast group: " + multicastAddress + ":" + port);
    }

    public void start() {
        new Thread(this::receiveMessages).start();

        //каждые 3 секунды отправляем сообщение
        scheduler.scheduleAtFixedRate(this::sendAliveMessage, 0, 3, TimeUnit.SECONDS);

        //проверяем таймауты раз в 5 сек
        scheduler.scheduleAtFixedRate(this::checkTimeouts, 0, 5, TimeUnit.SECONDS);
    }

    private void sendAliveMessage() {
        try {
            String message = "NODE_ALIVE:" + nodeId + ":" + System.currentTimeMillis();
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    private void receiveMessages() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (!socket.isClosed()) {
            try {
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                String ipAddress = packet.getAddress().getHostAddress();//метод InetAddress, возвращает стрроку из айпи

                if (message.startsWith("NODE_ALIVE:")) {
                    String[] parts = message.split(":");
                    if (parts.length == 3) {
                        String receivedNodeId = parts[1];
                        long timestamp = Long.parseLong(parts[2]);

                        //собственные сообщения игнорируем
                        if (!receivedNodeId.equals(nodeId)) {
                            activeNodes.put(receivedNodeId, new NodeInfo(ipAddress, timestamp));
                            printActiveNodes();
                        }
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.err.println("Error receiving message: " + e.getMessage());
                }
            }
        }
    }

    private void checkTimeouts() {
        long currentTime = System.currentTimeMillis();
        boolean changed = false;

        Iterator<Map.Entry<String, NodeInfo>> iterator = activeNodes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, NodeInfo> entry = iterator.next();
            if (currentTime - entry.getValue().lastSeen > timeoutMs) {
                iterator.remove();
                changed = true;
            }
        }

        if (changed) {
            printActiveNodes();
        }
    }

    private void printActiveNodes() {
        System.out.println("Active nodes:");
        if (activeNodes.isEmpty()) {
            System.out.println("  No active nodes.");
        } else {
            activeNodes.forEach((id, info) ->
                    System.out.println("  Node ID: " + id + ", IP: " + info.ipAddress));
        }
    }

    public void stop() throws IOException {
        socket.leaveGroup(new InetSocketAddress(group, port), NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
        socket.close();
        scheduler.shutdown();
        System.out.println("Node stopped.");
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter multicast address: ");
        String multicastAddress = sc.nextLine();
        System.out.print("Enter port: ");
        int port = Integer.parseInt(sc.nextLine());

        try {
            NodeDiscovery node = new NodeDiscovery(multicastAddress, port);
            node.start();

            Thread.sleep(60000);
            node.stop();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
// 224.2.2.4
// ff02::1