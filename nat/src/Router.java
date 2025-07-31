import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Router {
    private ServerSocket serverSocket;
    private Map<String, String> arpTable;
    private Map<String, ClientInfo> clients;
    private ExecutorService executor;
    private String publicIp;//
    private Map<String, String> natTable;//publicIp:port -> localIp

    public Router(String host, int port, String publicIp) throws IOException {
        serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host));
        clients = new ConcurrentHashMap<>();
        arpTable = new ConcurrentHashMap<>();
        natTable = new ConcurrentHashMap<>();
        executor = Executors.newCachedThreadPool();
        this.publicIp = publicIp;//
        System.out.println("Маршрутизатор запущен на " + host + ":" + port + " с публичным IP: " + publicIp);
    }

    public void start() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Подключён клиент: " + clientSocket.getInetAddress());
                executor.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String data = in.readLine();
            if (data != null && data.startsWith("REGISTER")) {
                String[] parts = data.split(" ");
                if (parts.length == 3) {
                    String ip = parts[1];
                    String mac = parts[2];
                    synchronized (clients) {
                        if (clients.containsKey(ip)) {
                            System.out.println("Конфликт IP: " + ip + " уже зарегистрирован!");
                            out.println("ERROR: IP уже используется");
                            return;
                        }
                        if (arpTable.containsValue(mac)) {
                            System.out.println("Конфликт MAC: " + mac + " уже используется!");
                            out.println("ERROR: MAC уже используется");
                            return;
                        }
                        clients.put(ip, new ClientInfo(clientSocket, mac, out));
                        arpTable.put(ip, mac);
                        System.out.println("Зарегистрирован клиент: " + ip + "/" + mac);
                        System.out.println("ARP-таблица: " + arpTable);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }

            String message;
            while ((message = in.readLine()) != null) {
                routeMessage(message, clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Ошибка клиента: " + e.getMessage());
        } finally {
            synchronized (clients) {
                for (Iterator<Map.Entry<String, ClientInfo>> it = clients.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<String, ClientInfo> entry = it.next();
                    if (entry.getValue().socket == clientSocket) {
                        String ip = entry.getKey();
                        String mac = entry.getValue().mac;
                        arpTable.remove(ip);
                        natTable.entrySet().removeIf(natEntry -> natEntry.getValue().equals(ip));
                        //из nat тоже удаляем
                        it.remove();
                        System.out.println("Клиент " + ip + "/" + mac + " отключён");
                        System.out.println("ARP-таблица: " + arpTable);
                        System.out.println("NAT-таблица: " + natTable);
                        break;
                    }
                }
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Ошибка при закрытии сокета: " + e.getMessage());
            }
        }
    }

    private void routeMessage(String message, Socket senderSocket) {
        synchronized (clients) {
            if (message.startsWith("ARP-REQUEST")) {
                String targetIp = message.split(" ")[1];
                System.out.println("Получен ARP-REQUEST для " + targetIp);
                String targetMac = arpTable.get(targetIp);
                ClientInfo sender = findClientBySocket(senderSocket);
                if (sender != null) {
                    if (targetMac != null) {
                        sender.out.println("ARP-REPLY " + targetMac);
                        System.out.println("Отправлен ARP-ответ: " + targetIp + " -> " + targetMac);
                    } else {
                        sender.out.println("ERROR1: IP " + targetIp + " не найден");
                    }
                }
            } else if (message.startsWith("PING ")) {
                //System.out.println("Получено: " + message);
                String[] parts = message.split(" ");
                if (parts.length == 3) {
                    String targetIp = parts[1];
                    String sourceIp = parts[2];
                    ClientInfo source = clients.get(sourceIp);
                    if (source == null) return;

                    if (isLocalIp(targetIp)) {
                        ClientInfo target = clients.get(targetIp);
                        if (target != null) {
                            System.out.println("Получен PING от " + sourceIp + " к локальному " + targetIp);
                            target.out.println("PING от " + sourceIp + "/" + arpTable.get(sourceIp));
                            source.out.println("PING доставлен на " + targetIp);
                        } else {
                            source.out.println("ERROR2: IP " + targetIp + " не найден");
                        }
                    } else {
                        //внешний айпи -> применяем nat
                        natTable.entrySet().removeIf(natEntry -> natEntry.getValue().equals(sourceIp));
                        String natPort = String.valueOf(new Random().nextInt(10000) + 10000);
                        String natKey = publicIp + ":" + natPort;
                        natTable.put(natKey, sourceIp);
                        System.out.println("NAT: " + sourceIp + " -> " + natKey + " для " + targetIp);
                        System.out.println("NAT-таблица: " + natTable);
                        ClientInfo target = clients.get(targetIp);
                        if (target != null) {
                            target.out.println("PING от " + natKey + "/" + arpTable.get(sourceIp));
                            source.out.println("PING доставлен на " + targetIp + " через NAT");
                        } else {
                            source.out.println("ERROR3: IP " + targetIp + " не найден");
                        }
                    }
                }
            } else if (message.startsWith("PING-REPLY")) {
                String[] parts = message.split(" ");
                if (parts.length == 3) {
                    String sourceIp = parts[1]; //может быть publicIp:port или локальный ip
                    String targetIp = parts[2];
                    ClientInfo sender = findClientBySocket(senderSocket);
                    if (sender == null) {
                        return;
                    }

                    //проверяем является ли sourceIp публичным с портом
                    String localIp = natTable.get(sourceIp);
                    if (localIp != null) {
                        ClientInfo target = clients.get(localIp);
                        if (target != null) {
                            target.out.println("PING-REPLY от " + targetIp);
                            System.out.println("Отправлен PING-REPLY на " + localIp + " через NAT от " + targetIp);
                        } else {
                            System.out.println("Ошибка: локальный IP " + localIp + " не найден в clients");
                        }
                    } else {
                        //локальная маршрутизация
                        ClientInfo target = clients.get(targetIp);
                        if (target != null) {
                            target.out.println("PING-REPLY от " + sourceIp + "/" + arpTable.get(sourceIp));
                            System.out.println("Отправлен PING-REPLY на " + targetIp);
                        } else {
                            System.out.println("Ошибка: целевой IP " + targetIp + " не найден в clients");
                        }
                    }
                }
            }
        }
    }

    //диапазоны локальных адресов
    private boolean isLocalIp(String ip) {
        String cleanIp = ip.contains(":") ? ip.split(":")[0] : ip;
        return cleanIp.startsWith("192.168.") || cleanIp.startsWith("10.") ||
                cleanIp.startsWith("172.16.") || cleanIp.startsWith("172.17.") ||
                cleanIp.startsWith("172.18.") || cleanIp.startsWith("172.19.") ||
                cleanIp.startsWith("172.20.") || cleanIp.startsWith("172.21.") ||
                cleanIp.startsWith("172.22.") || cleanIp.startsWith("172.23.") ||
                cleanIp.startsWith("172.24.") || cleanIp.startsWith("172.25.") ||
                cleanIp.startsWith("172.26.") || cleanIp.startsWith("172.27.") ||
                cleanIp.startsWith("172.28.") || cleanIp.startsWith("172.29.") ||
                cleanIp.startsWith("172.30.") || cleanIp.startsWith("172.31.");
    }

    private ClientInfo findClientBySocket(Socket socket) {
        for (ClientInfo info : clients.values()) {
            if (info.socket == socket) return info;
        }
        return null;
    }

    public void stop() {
        try {
            executor.shutdown();
            serverSocket.close();
            synchronized (clients) {
                for (ClientInfo client : clients.values()) {
                    client.socket.close();
                }
                clients.clear();
                arpTable.clear();
                natTable.clear();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при остановке: " + e.getMessage());
        }
    }

    private static class ClientInfo {
        Socket socket;
        String mac;
        PrintWriter out;

        ClientInfo(Socket socket, String mac, PrintWriter out) {
            this.socket = socket;
            this.mac = mac;
            this.out = out;
        }
    }

    public static void main(String[] args) throws IOException {
        try {
            Router router = new Router("localhost", 9999, "203.0.113.1");
            router.start();
        } catch (IOException e) {
            System.err.println("Не удалось запустить маршрутизатор: " + e.getMessage());
        }
    }
}
/*
203.0.113.0–203.0.113.255 : Assigned as TEST-NET-3, documentation and examples
 */