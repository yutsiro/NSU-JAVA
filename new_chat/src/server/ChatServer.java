package server;

import common.*;
import common.msg.ChatMessage;
import common.msg.EventMessage;
import common.msg.ListResponse;
import server.ClientHandler;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

public class ChatServer {
    private static final Logger LOGGER = Logger.getLogger(ChatServer.class.getName());
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private List<ChatMessage> messageHistory = Collections.synchronizedList(new ArrayList<>());
    private int port;
    private boolean logging;
    private String protocol;
    private long timeout;
    private FileHandler fileHandler;

    public ChatServer() throws Exception {
        loadConfig();
        setupLogging();
        serverSocket = new ServerSocket(port);
        System.out.println("Сервер запущен на порту " + port);
        log("Сервер запущен на порту " + port);

        new Thread(new TimeoutChecker(this, timeout)).start();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            log("Новое подключение от: " + clientSocket.getInetAddress());
            ClientHandler clientHandler = new ClientHandler(clientSocket, this, protocol);
            synchronized (clients) {
                clients.add(clientHandler);
            }
            new Thread(clientHandler).start();
        }
    }

    private void loadConfig() throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
        }
        port = Integer.parseInt(props.getProperty("port", "12345"));
        logging = Boolean.parseBoolean(props.getProperty("logging", "false"));
        protocol = props.getProperty("protocol", "serialization");
        timeout = Integer.parseInt(props.getProperty("timeout", "60")) * 1000L;
    }

    private void setupLogging() throws IOException {
        if (logging) {
            fileHandler = new FileHandler("server.log", false);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
        }
    }

    public void log(String message) {
        if (logging) {
            LOGGER.info(message);
        }
    }

    public void broadcastMessage(ChatMessage message, ClientHandler sender) {
        synchronized (messageHistory) {
            messageHistory.add(message);
            log("Добавлено сообщение в историю: " + message.getText() + " от " + message.getSender());
        }
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(new ChatMessage(message.getSender(), message.getText(), null));
            }
        }
    }

    public void broadcastEvent(String event, String name) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(new EventMessage(event, name));
            }
        }
        broadcastUserList();
    }

    public void broadcastUserList() {
        synchronized (clients) {
            String[] users = clients.stream()
                    .map(ClientHandler::getClientName)
                    .filter(Objects::nonNull)
                    .toArray(String[]::new);
            for (ClientHandler client : clients) {
                client.sendMessage(new ListResponse(users));
            }
        }
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public List<ChatMessage> getMessageHistory() {
        return messageHistory;
    }

    public void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
        }
    }

    public static void main(String[] args) throws Exception {
        new ChatServer();
    }
}