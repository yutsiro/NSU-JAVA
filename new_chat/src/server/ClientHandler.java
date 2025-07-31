package server;

import common.*;
import common.msg.*;
import org.w3c.dom.Document;

import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private String clientName;
    private String sessionId;
    private MessageProcessor processor;
    private boolean running = true;
    private long lastActivityTime;
    private IMessageHandler messageHandler;
    private final ChatServer server;
    private final String protocol;

    public ClientHandler(Socket socket, ChatServer server, String protocol) {
        this.socket = socket;
        this.server = server;
        this.protocol = protocol;
        this.lastActivityTime = System.currentTimeMillis();
        this.messageHandler = new ServerMessageHandler(this, server);
        try {
            if (this.protocol.equals("serialization")) {
                processor = new MessageProcessor(new SerializationProtocol(socket));
                server.log("ClientHandler: Создан MessageProcessor для сериализации");
            } else {
                processor = new MessageProcessor(new XMLProtocol(socket));
                server.log("ClientHandler: Создан MessageProcessor для XML");
            }
        } catch (IOException e) {
            server.log("ClientHandler: Ошибка инициализации протокола: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ex) {
                server.log("ClientHandler: Ошибка закрытия сокета: " + ex.getMessage());
            }
        }
    }

    public String getClientName() {
        return clientName;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    void setClientName(String clientName) {
        this.clientName = clientName;
    }

    void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        server.log("ClientHandler.run: Начало обработки клиента " + (clientName != null ? clientName : "без имени"));
        try {
            while (running) {
                server.log("ClientHandler.run: Ожидание сообщения...");
                Object message = processor.receiveMessage();
                server.log("ClientHandler.run: Получено сообщение: " + message);
                Object processed = processor.processMessage(message);
                server.log("ClientHandler.run: Обработанное сообщение: " + processed);
                if (this.protocol.equals("serialization") && processed instanceof Message) {
                    server.log("ClientHandler.run: Передача в messageHandler (сериализация)");
                    messageHandler.handle(new MessageContext((Message) processed, "serialization"));
                } else if (this.protocol.equals("xml") && processed instanceof Document) {
                    server.log("ClientHandler.run: Передача в messageHandler (XML)");
                    messageHandler.handle(new MessageContext((Document) processed, "xml"));
                }
                lastActivityTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            server.log("ClientHandler.run: Ошибка клиента " + clientName + ": " + e.getMessage());
            if (e.getMessage().contains("Invalid message length")) {
                try {
                    processor.sendMessage(new ErrorMessage("Некорректное сообщение: " + e.getMessage()));
                } catch (IOException ex) {
                    server.log("ClientHandler.run: Ошибка отправки ErrorMessage: " + ex.getMessage());
                }
            }
            running = false;
        } finally {
            disconnect();
        }
    }

    public void sendMessage(Message msg) {
        try {
            server.log("ClientHandler.sendMessage: Отправка сообщения клиенту " + clientName + ": " + msg);
            processor.sendMessage(msg);
            server.log("ClientHandler.sendMessage: Сообщение успешно отправлено");
        } catch (IOException e) {
            server.log("ClientHandler.sendMessage: Ошибка отправки сообщения клиенту " + clientName + ": " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            server.log("ClientHandler.disconnect: Начало отключения клиента " + clientName);
            server.removeClient(this);
            if (clientName != null) {
                server.broadcastEvent("userlogout", clientName);
                server.log("ClientHandler.disconnect: Клиент " + clientName + " отключился");
            }
            processor.close();
            socket.close();
            server.log("ClientHandler.disconnect: Сокет закрыт");
        } catch (IOException e) {
            server.log("ClientHandler.disconnect: Ошибка при отключении клиента " + clientName + ": " + e.getMessage());
        }
    }
}