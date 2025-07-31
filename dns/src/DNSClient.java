import java.io.*;
import java.net.*;

public class DNSClient {
    private static final String DNS_SERVER_ADDRESS = "127.0.0.1";
    private static final int DNS_SERVER_PORT = 50000;
    private static final String MULTICAST_ADDRESS = "239.255.255.250";
    private static final int MULTICAST_PORT = 5353;
    private static final int DISCOVERY_TIMEOUT = 3000;//3сек
    private String dnsServerAddress;
    private int dnsServerPort;

    public DNSClient() throws IOException {
        try {
            String[] serverInfo = discoverDNSServer();
            this.dnsServerAddress = serverInfo[0];
            this.dnsServerPort = Integer.parseInt(serverInfo[1]);
            System.out.println("Discovered DNS server: " + dnsServerAddress + ":" + dnsServerPort);
        } catch (IOException e) {
            System.out.println("DNS discovery failed, falling back to default: " + DNS_SERVER_ADDRESS + ":" + DNS_SERVER_PORT);
            this.dnsServerAddress = DNS_SERVER_ADDRESS;
            this.dnsServerPort = DNS_SERVER_PORT;
        }
    }

    private String[] discoverDNSServer() throws IOException {
        try (MulticastSocket socket = new MulticastSocket()) {
            socket.setSoTimeout(DISCOVERY_TIMEOUT);
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);

            String discoveryRequest = "DISCOVER_DNS";//запрос обнаружения
            byte[] requestBytes = discoveryRequest.getBytes();
            DatagramPacket packet = new DatagramPacket(requestBytes, requestBytes.length, group, MULTICAST_PORT);
            socket.send(packet);

            byte[] buffer = new byte[1024];//ждем ответ в виде <ip>:<port>
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(responsePacket);
            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            socket.leaveGroup(group);

            if (!response.matches(".+:\\d+")) {
                throw new IOException("Invalid DNS server response format: " + response);
            }
            return response.split(":");
        }
    }

    public void register(String domain, String address) throws IOException {
        sendRequest("REGISTER:" + domain + ":" + address);
    }

    public String resolve(String domain) throws IOException {
        return sendRequest("RESOLVE:" + domain);
    }

    private String sendRequest(String request) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] requestBytes = request.getBytes();
            DatagramPacket packet = new DatagramPacket(requestBytes, requestBytes.length,
                    InetAddress.getByName(dnsServerAddress), dnsServerPort);
            socket.send(packet);

            byte[] buffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(responsePacket);
            return new String(responsePacket.getData(), 0, responsePacket.getLength());
        }
    }
}