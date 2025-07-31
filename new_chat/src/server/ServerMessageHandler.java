package server;

import common.*;
import common.msg.*;
import common.msg.Message.*;
import org.w3c.dom.*;

import java.util.Objects;
import java.util.UUID;

public class ServerMessageHandler implements IMessageHandler {
    private final ClientHandler clientHandler;
    private final ChatServer server;

    public ServerMessageHandler(ClientHandler clientHandler, ChatServer server) {
        this.clientHandler = clientHandler;
        this.server = server;
        server.log("ServerMessageHandler: Создан для клиента " + clientHandler.getClientName());
    }

    @Override
    public void handle(MessageContext context) throws Exception {
        String protocol = context.getProtocol();
        server.log("ServerMessageHandler.handle: Начало обработки, протокол: " + protocol);
        if (protocol.equals("serialization")) {
            Message msg = context.getMessage();
            if (msg == null) {
                server.log("ServerMessageHandler.handle: Сообщение null (сериализация)");
                return;
            }
            handleSerialization(msg);
        } else if (protocol.equals("xml")) {
            Document doc = context.getXmlDocument();
            if (doc == null) {
                server.log("ServerMessageHandler.handle: Документ null (XML)");
                return;
            }
            handleXML(doc);
        }
    }

    private void handleSerialization(Message msg) throws Exception {
        String command = msg.getCommand();
        server.log("ServerMessageHandler.handleSerialization: Получена команда: " + command + " от " + clientHandler.getClientName());

        if (!command.equals("login") && clientHandler.getSessionId() == null) {
            server.log("ServerMessageHandler.handleSerialization: Клиент не авторизован");
            clientHandler.sendMessage(new ErrorMessage("Не выполнен вход. Требуется login."));
            return;
        }

        switch (command) {
            case "login":
                if (msg instanceof LoginMessage) {
                    LoginMessage loginMsg = (LoginMessage) msg;
                    server.log("ServerMessageHandler.handleSerialization: Обработка login для имени: " + loginMsg.getName());
                    synchronized (server.getClients()) {
                        if (server.getClients().stream().anyMatch(c -> loginMsg.getName().equals(c.getClientName()))) {
                            server.log("ServerMessageHandler.handleSerialization: Имя занято: " + loginMsg.getName());
                            clientHandler.sendMessage(new ErrorMessage("Имя " + loginMsg.getName() + " уже занято."));
                            return;
                        }
                    }
                    clientHandler.setClientName(loginMsg.getName());
                    clientHandler.setSessionId(UUID.randomUUID().toString());
                    server.log("ServerMessageHandler.handleSerialization: Установлено имя: " + loginMsg.getName() + ", sessionId: " + clientHandler.getSessionId());
                    clientHandler.sendMessage(new SuccessMessage(clientHandler.getSessionId()));
                    server.broadcastEvent("userlogin", clientHandler.getClientName());
                    server.log("ServerMessageHandler.handleSerialization: Клиент " + clientHandler.getClientName() + " подключился");
                    synchronized (server.getMessageHistory()) {
                        for (ChatMessage pastMsg : server.getMessageHistory()) {
                            server.log("ServerMessageHandler.handleSerialization: Отправка истории: " + pastMsg.getText());
                            clientHandler.sendMessage(new ChatMessage(pastMsg.getSender(), pastMsg.getText(), null));
                        }
                    }
                }
                break;
            case "list":
                if (msg instanceof ListRequest) {
                    server.log("ServerMessageHandler.handleSerialization: Запрос списка пользователей");
                    String[] users;
                    synchronized (server.getClients()) {
                        users = server.getClients().stream().map(ClientHandler::getClientName).filter(Objects::nonNull).toArray(String[]::new);
                    }
                    clientHandler.sendMessage(new ListResponse(users));
                    server.log("ServerMessageHandler.handleSerialization: Список пользователей отправлен");
                }
                break;
            case "message":
                if (msg instanceof ChatMessage) {
                    ChatMessage chatMsg = (ChatMessage) msg;
                    server.log("ServerMessageHandler.handleSerialization: Получено сообщение: " + chatMsg.getText());
                    server.broadcastMessage(new ChatMessage(clientHandler.getClientName(), chatMsg.getText(), null), clientHandler);
                    clientHandler.sendMessage(new SuccessMessage(null));
                }
                break;
            case "logout":
                if (msg instanceof LogoutMessage) {
                    server.log("ServerMessageHandler.handleSerialization: Обработка logout");
                    clientHandler.setRunning(false);
                    clientHandler.sendMessage(new SuccessMessage(null));
                }
                break;
        }
    }

    private void handleXML(Document doc) throws Exception {
        Element root = doc.getDocumentElement();
        String command = root.getAttribute("name");
        server.log("ServerMessageHandler.handleXML: Получена команда: " + command + " от " + clientHandler.getClientName());

        if (!command.equals("login") && clientHandler.getSessionId() == null) {
            server.log("ServerMessageHandler.handleXML: Клиент не авторизован");
            clientHandler.sendMessage(new ErrorMessage("Не выполнен вход. Требуется login."));
            return;
        }

        switch (command) {
            case "login":
                NodeList nameNodes = root.getElementsByTagName("name");
                String name = nameNodes.getLength() > 0 ? nameNodes.item(0).getTextContent() : null;
                server.log("ServerMessageHandler.handleXML: Обработка login для имени: " + name);
                synchronized (server.getClients()) {
                    if (server.getClients().stream().anyMatch(c -> name.equals(c.getClientName()))) {
                        server.log("ServerMessageHandler.handleXML: Имя занято: " + name);
                        clientHandler.sendMessage(new ErrorMessage("Имя " + name + " уже занято."));
                        return;
                    }
                }
                clientHandler.setClientName(name);
                clientHandler.setSessionId(UUID.randomUUID().toString());
                server.log("ServerMessageHandler.handleXML: Установлено имя: " + name + ", sessionId: " + clientHandler.getSessionId());
                clientHandler.sendMessage(new SuccessMessage(clientHandler.getSessionId()));
                server.broadcastEvent("userlogin", clientHandler.getClientName());
                server.log("ServerMessageHandler.handleXML: Клиент " + clientHandler.getClientName() + " подключился");
                synchronized (server.getMessageHistory()) {
                    for (ChatMessage pastMsg : server.getMessageHistory()) {
                        server.log("ServerMessageHandler.handleXML: Отправка истории: " + pastMsg.getText());
                        clientHandler.sendMessage(new ChatMessage(pastMsg.getSender(), pastMsg.getText(), null));
                    }
                }
                break;
            case "list":
                server.log("ServerMessageHandler.handleXML: Запрос списка пользователей");
                String[] users;
                synchronized (server.getClients()) {
                    users = server.getClients().stream().map(ClientHandler::getClientName).filter(Objects::nonNull).toArray(String[]::new);
                }
                clientHandler.sendMessage(new ListResponse(users));
                server.log("ServerMessageHandler.handleXML: Список пользователей отправлен");
                break;
            case "message":
                NodeList msgNodes = root.getElementsByTagName("message");
                if (msgNodes.getLength() > 0) {
                    String message = msgNodes.item(0).getTextContent();
                    server.log("ServerMessageHandler.handleXML: Получено сообщение: " + message);
                    server.broadcastMessage(new ChatMessage(clientHandler.getClientName(), message, null), clientHandler);
                    clientHandler.sendMessage(new SuccessMessage(null));
                }
                break;
            case "logout":
                server.log("ServerMessageHandler.handleXML: Обработка logout");
                clientHandler.setRunning(false);
                clientHandler.sendMessage(new SuccessMessage(null));
                break;
        }
    }
}