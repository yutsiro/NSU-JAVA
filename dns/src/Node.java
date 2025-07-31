import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Node {
    private final String nodeName;
    private final int port;
    private DNSClient dnsClient;

    public Node(String nodeName, int port) throws IOException {
        this.nodeName = nodeName;
        this.port = port;
        initializeDNSClient();
    }

    private void initializeDNSClient() throws IOException {
        this.dnsClient = new DNSClient();
    }

    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Node " + nodeName + " started HTTP server on port " + port);

        dnsClient.register(nodeName + ".local", "127.0.0.1:" + port);

        new Thread(() -> {
            try {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    handleClient(clientSocket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(Socket clientSocket) {
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println();
            out.println("<html><body><h1>Welcome to " + nodeName + "</h1></body></html>");
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestPage(String targetDomain) throws IOException {
        String nodeAddress = dnsClient.resolve(targetDomain);
        System.out.println("Resolved address for " + targetDomain + ": " + nodeAddress);
        if (!nodeAddress.equals("Not found")) {
            String[] addrParts = nodeAddress.split(":");
            if (addrParts.length < 2) {
                throw new IOException("Invalid address format for " + targetDomain + ": " + nodeAddress);
            }
            String ip = addrParts[0];
            int port = Integer.parseInt(addrParts[1]);
            Socket socket = new Socket(ip, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            System.out.println("Response from " + targetDomain + ":");
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            socket.close();
        } else {
            System.out.println("Node " + targetDomain + " not found");
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter node name (e.g., node1): ");
        String nodeName = scanner.nextLine().trim();
        if (nodeName.isEmpty()) {
            System.out.println("Node name cannot be empty. Using default: node1");
            nodeName = "node1";
        }

        int port = 0;
        while (port <= 0) {
            System.out.print("Enter port number (e.g., 8081): ");
            try {
                port = Integer.parseInt(scanner.nextLine().trim());
                if (port < 1024 || port > 65535) {
                    System.out.println("Port must be between 1024 and 65535");
                    port = 0;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Please enter a valid number.");
            }
        }

        try {
            Node node = new Node(nodeName, port);
            node.startServer();

            System.out.print("Enter target node domain to request page (e.g., node2.local) or press Enter to skip: ");
            String targetDomain = scanner.nextLine().trim();
            if (!targetDomain.isEmpty()) {
                node.requestPage(targetDomain);
            } else {
                System.out.println("Skipping page request.");
            }

        } catch (IOException e) {
            System.err.println("Error starting node: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}