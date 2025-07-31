import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;//для многотхритности

//todo : если соединение во время пинга было прервано (+ответ от целевого клиента)
public class Router {
    private ServerSocket serverSocket;//серверный сокет для принятия подключений клиентов
    private Map<String, ClientInfo> clients; // ip -> объект ClientInfo(socket, mac, out)
    private Map<String, String> arpTable; // ip -> mac
    private ExecutorService executor;//пул потоков

    public Router(String host, int port) throws IOException {
        serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host));//серв.сокет, максимум 50 подключений
        clients = new ConcurrentHashMap<>();//безопасно для многопоточности (айпи+инфа про клиент)
        arpTable = new ConcurrentHashMap<>();//мапы для клиентов и арп соответствий (айпи+мас)
        executor = Executors.newCachedThreadPool();//пул потоков для обработки клиентов
        System.out.println("Маршрутизатор запущен на " + host + ":" + port);
    }

    //запуск роутера, слушаем подключения
    public void start() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();//ждем клиента и возвращаем сокет для общения с ним
                executor.execute(() -> handleClient(clientSocket));//в отдельном потоке обрабатываем клиента
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        } finally {
            stop();
        }
    }

    //обработка ОДНОГО клиента
    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            //in - читаем строки от клиента (его сообщения)
            //out - отправляем мсджи

            String data = in.readLine();//ожидается команда от клиента для регистрации

            if (data != null && data.startsWith("REGISTER")) {
                String[] parts = data.split(" ");//изввлекаем айпи и мак-адрес из команды
                if (parts.length == 3) {
                    String ip = parts[1];
                    String mac = parts[2];
                    synchronized (clients) {//увиливаем от гонки данных (только один поток может выполнять этот блок кода)
                        //проверка на конфликт IP - ошибочька вышла
                        if (clients.containsKey(ip)) {
                            System.out.println("Конфликт IP: " + ip + " уже зарегистрирован!");
                            out.println("ERROR: IP уже используется");
                            return;
                        }
                        //проверка на конфликт MAC - тоже ошибочька хотя в реальной сети ни то ни это не контрится
                        if (arpTable.containsValue(mac)) {
                            System.out.println("Конфликт MAC: " + mac + " уже используется!");
                            out.println("ERROR: MAC уже используется");
                            return;
                        }
                        //добавляем клиент в табличку с айпи и в арп таблицу
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

            //обрабатываем соо от клиента
            String message;
            while ((message = in.readLine()) != null) {
                routeMessage(message, clientSocket);//передаем соо для обработки
            }
        } catch (IOException e) {
            System.err.println("Ошибка клиента: " + e.getMessage());
        } finally {
            //сносим клиента
            synchronized (clients) {//ниже: перебираем все записи в clients
                for (Iterator<Map.Entry<String, ClientInfo>> it = clients.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<String, ClientInfo> entry = it.next();//некст запись из clients
                    if (entry.getValue().socket == clientSocket) {//сокет записи == сокет отключившегося клиента?
                        String ip = entry.getKey();//удаляем из арп-т по айпи
                        String mac = entry.getValue().mac;
                        arpTable.remove(ip);
                        it.remove();
                        System.out.println("Клиент " + ip + "/" + mac + " отключён");
                        System.out.println("ARP-таблица: " + arpTable);
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

    //обработка сообщений
    private void routeMessage(String message, Socket senderSocket) {
        synchronized (clients) {
            if (message.startsWith("ARP-REQUEST")) {
                String targetIp = message.split(" ")[1];
                System.out.println("Получен ARP-REQUEST");
                String targetMac = arpTable.get(targetIp);//ищем мас-адрес в роутинг таблице
                ClientInfo sender = findClientBySocket(senderSocket);//ищем отправителя по сокету
                if (sender != null) {
                    if (targetMac != null) {//нашли мас-адрес -- отправляем ответ
                        sender.out.println("ARP-REPLY " + targetMac);
                        System.out.println("Отправлен ARP-ответ: " + targetIp + " -> " + targetMac);
                    } else {
                        sender.out.println("ERROR: IP " + targetIp + " не найден");
                    }
                }
            } else if (message.startsWith("PING")) {
                String[] parts = message.split(" ");
                if (parts.length == 3) {//формат: PING <targetIp> <sourceIp>
                    String targetIp = parts[1];
                    String sourceIp = parts[2];
                    ClientInfo target = clients.get(targetIp);
                    ClientInfo source = clients.get(sourceIp);
                    if (target != null && source != null) {
                        System.out.println("Получен PING от " + sourceIp);
                        target.out.println("PING от " + sourceIp + "/" + arpTable.get(sourceIp));
                        source.out.println("PING доставлен на " + targetIp);
                    } else if (source != null) {
                        source.out.println("ERROR: IP " + targetIp + " не найден");
                    }
                }
            } else if (message.startsWith("PING_REPLY")) {
                String[] parts = message.split(" ");
                if (parts.length == 3) {
                    String sourceIp = parts[1];
                    String targetIp = parts[2];
                    ClientInfo source = clients.get(sourceIp);
                    ClientInfo sender = findClientBySocket(senderSocket);
                    source.out.println("PING-REPLY от " + sourceIp + "/" + arpTable.get(sourceIp));
                }
            }
        }
    }

    //ищем клиент по сокету -- возвращаем либо нулл либо клиент
    ClientInfo findClientBySocket(Socket socket) {
        for (ClientInfo info : clients.values()) {
            if (info.socket == socket) return info;
        }
        return null;
    }

    //очистка всех ресурсов
    public void stop() {
        try {
            executor.shutdown();//останавливаем пул потоков
            serverSocket.close();//закрываем сокет у сервера
            synchronized (clients) {//клиентские сокеты тоже
                for (ClientInfo client : clients.values()) {
                    client.socket.close();
                }
                clients.clear();
                arpTable.clear();
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

    public static void main(String[] args) {
        try {
            Router router = new Router("localhost", 9999);
            router.start();
        } catch (IOException e) {
            System.err.println("Не удалось запустить маршрутизатор: " + e.getMessage());
        }
    }
}