package client;

import common.*;
import common.msg.*;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Properties;

public class ChatClient {
    private Socket socket;
    private MessageProcessor processor;
    private String clientName;
    private String sessionId;
    private String protocol;
    private boolean running = true;
    private ChatUI ui;
    private IMessageHandler messageHandler;

    public ChatClient() {
        loadConfig();
        ui = new ChatUI();
        messageHandler = new ClientMessageHandler(this, ui);
        setupUIActions();
        connectToServer();
        ui.setVisible(true);
    }

    private void loadConfig() {
        try {
            Properties props = new Properties();
            try (FileReader fis = new FileReader("config.properties")) {
                props.load(fis);
            }
            protocol = props.getProperty("protocol", "serialization");
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    private void setupUIActions() {
        ui.setOnSendMessage(this::sendMessage);
        ui.setOnRequestUserList(this::requestUserList);
        ui.setOnLogout(this::logout);
    }

    private void connectToServer() {
        String host = JOptionPane.showInputDialog("Введите адрес сервера:", "localhost");
        String portStr = JOptionPane.showInputDialog("Введите порт сервера:", "12345");
        clientName = JOptionPane.showInputDialog(null, "Введите ваш ник:");
        try {
            socket = new Socket(host, Integer.parseInt(portStr));
            processor = new MessageProcessor(protocol.equals("serialization") ? new SerializationProtocol(socket) : new XMLProtocol(socket));
            processor.sendMessage(new LoginMessage(clientName, "CHAT_CLIENT"));
            new Thread(this::receiveMessages).start();
        } catch (IOException e) {
            ui.appendMessage("System", "Ошибка подключения: " + e.getMessage() + "\n", Color.RED);
            running = false;
        }
    }

    private void receiveMessages() {
        try {
            while (running) {
                Object message = processor.receiveMessage();
                if (message == null) {
                    break;
                }
                Object processed = processor.processMessage(message);
                MessageContext context = protocol.equals("serialization")
                        ? new MessageContext((Message) processed, "serialization")
                        : new MessageContext((Document) processed, "xml");
                messageHandler.handle(context);
            }
        } catch (Exception e) {
            if (running) {
                ui.appendMessage("System", "Ошибка получения сообщения: " + e.getMessage() + "\n", Color.RED);
                running = false;
            }
        } finally {
            if (running) {
                logout();
            }
        }
    }

    private void sendMessage() {
        String text = ui.getMessageText();
        if (!text.isEmpty()) {
            try {
                processor.sendMessage(new ChatMessage(clientName, text, sessionId));
                ui.clearMessageField();
            } catch (IOException e) {
                ui.appendMessage("System", "Ошибка отправки: " + e.getMessage() + "\n", Color.RED);
                running = false;
                logout();
            }
        }
    }

    private void requestUserList() {
        System.out.println("ChatClient.requestUserList: Запрос списка пользователей");
        try {
            processor.sendMessage(new ListRequest(sessionId));
        } catch (IOException e) {
            ui.appendMessage("System", "Ошибка запроса списка: " + e.getMessage() + "\n", Color.RED);
            running = false;
            logout();
        }
    }

    private void logout() {
        running = false;
        try {
            processor.sendMessage(new LogoutMessage());
            processor.close();
            socket.close();
        } catch (IOException e) {
            ui.appendMessage("System", "Ошибка при отключении: " + e.getMessage() + "\n", Color.RED);
        }
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::new);
    }
}