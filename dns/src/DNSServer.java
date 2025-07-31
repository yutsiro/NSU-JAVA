import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class DNSServer {
    private static final int PORT = 50000;
    private static final String MULTICAST_ADDRESS = "239.255.255.250";
    private static final int MULTICAST_PORT = 5353;
    private Map<String, String> dnsRecords = new HashMap<>();
    private DatagramSocket socket;

    public DNSServer() throws IOException {
        socket = new DatagramSocket(PORT);
//        dnsRecords.put("node1.local", "127.0.0.1:8081");
//        dnsRecords.put("node2.local", "127.0.0.1:8082");
//        System.out.println("Initial dnsRecords: " + dnsRecords);
    }

    public void start() throws IOException {
        System.out.println("DNS Server started on port " + PORT);
        startMulticastListener();//!!
        while (true) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String request = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received DNS request: " + request);
            String[] parts = request.split(":", 3);
            String response;

            if (parts[0].equals("REGISTER")) {
                if (parts.length < 3) {
                    response = "Invalid REGISTER format. Expected: REGISTER:domain:ip:port";
                } else {
                    String domain = parts[1];
                    String address = parts[2]; // parts[2] уже содержит ip:port
                    if (!address.matches(".+:\\d+")) {
                        response = "Invalid address format. Expected: ip:port, got: " + address;
                    } else {
                        dnsRecords.put(domain, address);
                        response = "Registered: " + domain + " -> " + address;
                        System.out.println("Updated dnsRecords: " + dnsRecords);
                    }
                }
            } else if (parts[0].equals("RESOLVE")) {
                String domain = parts[1];
                response = dnsRecords.getOrDefault(domain, "Not found");
                if (!response.equals("Not found") && !response.matches(".+:\\d+")) {
                    response = "Not found";
                }
                System.out.println("Sending RESOLVE response for " + domain + ": " + response);
            } else {
                response = "Invalid request";
            }

            byte[] responseBytes = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length,
                    packet.getAddress(), packet.getPort());
            socket.send(responsePacket);
        }
    }

    private void startMulticastListener() {
        new Thread(() -> {
            try (MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT)) {
                InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
                multicastSocket.joinGroup(group);
                System.out.println("Multicast listener started on " + MULTICAST_ADDRESS + ":" + MULTICAST_PORT);

                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    multicastSocket.receive(packet);
                    String request = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Received multicast request: " + request);

                    if (request.equals("DISCOVER_DNS")) {
                        String response = "127.0.0.1:" + PORT;
                        byte[] responseBytes = response.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length,
                                packet.getAddress(), packet.getPort());
                        multicastSocket.send(responsePacket);
                        System.out.println("Sent multicast response: " + response);
                    }
                }
            } catch (IOException e) {
                System.err.println("Multicast listener error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) throws IOException {
        new DNSServer().start();
    }
}