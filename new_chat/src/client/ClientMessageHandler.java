package client;

import common.*;
import common.msg.*;
import org.w3c.dom.*;
import java.awt.Color;
import java.util.Arrays;

public class ClientMessageHandler implements IMessageHandler {
    private final ChatClient client;
    private final ChatUI ui;

    public ClientMessageHandler(ChatClient client, ChatUI ui) {
        this.client = client;
        this.ui = ui;
    }

    @Override
    public void handle(MessageContext context) throws Exception {
        String protocol = context.getProtocol();
        if (protocol.equals("serialization")) {
            Message msg = context.getMessage();
            if (msg == null) {
                return;
            }
            handleSerialization(msg);
        } else if (protocol.equals("xml")) {
            Document doc = context.getXmlDocument();
            if (doc == null) {
                return;
            }
            handleXML(doc);
        }
    }

    private void handleSerialization(Message msg) {
        switch (msg.getCommand()) {
            case "success":
                if (msg instanceof SuccessMessage) {
                    client.setSessionId(((SuccessMessage) msg).getSessionId());
                    ui.appendMessage("System", "Успешно подключились!\n", Color.BLUE);
                }
                break;
            case "message":
                if (msg instanceof ChatMessage) {
                    ChatMessage chat = (ChatMessage) msg;
                    String sender = chat.getSender() != null ? chat.getSender() : "Неизвестный пользователь";
                    ui.appendMessage(sender, chat.getText() + "\n", null);
                }
                break;
            case "userlogin":
                if (msg instanceof EventMessage) {
                    String name = ((EventMessage) msg).getUserName();
                    ui.appendMessage("System", "Пользователь " + name + " подключился\n", Color.GREEN);
                }
                break;
            case "userlogout":
                if (msg instanceof EventMessage) {
                    String name = ((EventMessage) msg).getUserName();
                    ui.appendMessage("System", "Пользователь " + name + " отключился\n", Color.RED);
                }
                break;
            case "error":
                if (msg instanceof ErrorMessage) {
                    ui.appendMessage("System", "Ошибка: " + ((ErrorMessage) msg).getErrorText() + "\n", Color.RED);
                }
                break;
            case "list":
                if (msg instanceof ListResponse) {
                    ui.updateUserList(((ListResponse) msg).getUsers());
                }
                break;
        }
    }

    private void handleXML(Document doc) {
        Element root = doc.getDocumentElement();
        String name = root.getAttribute("name");
        if (root.getTagName().equals("success")) {
            NodeList sessionNodes = root.getElementsByTagName("session");
            if (sessionNodes.getLength() > 0 && sessionNodes.item(0) != null) {
                client.setSessionId(sessionNodes.item(0).getTextContent());
                ui.appendMessage("System", "Успешно подключились!\n", Color.BLUE);
            }
            NodeList listUsers = root.getElementsByTagName("listusers");
            if (listUsers.getLength() > 0) {
                NodeList users = root.getElementsByTagName("user");
                String[] userNames = new String[users.getLength()];
                for (int i = 0; i < users.getLength(); i++) {
                    Element user = (Element) users.item(i);
                    NodeList nameNodes = user.getElementsByTagName("name");
                    if (nameNodes.getLength() > 0 && nameNodes.item(0) != null) {
                        userNames[i] = nameNodes.item(0).getTextContent();
                    }
                }
                ui.updateUserList(userNames);
            }
        } else if (root.getTagName().equals("event")) {
            NodeList nameNodes = root.getElementsByTagName("name");
            String userName = nameNodes.getLength() > 0 && nameNodes.item(0) != null ? nameNodes.item(0).getTextContent() : "Неизвестный пользователь";
            if (name.equals("message")) {
                NodeList messageNodes = root.getElementsByTagName("message");
                if (messageNodes.getLength() > 0 && messageNodes.item(0) != null) {
                    String messageText = messageNodes.item(0).getTextContent();
                    ui.appendMessage(userName, messageText + "\n", null);
                }
            } else if (name.equals("userlogin")) {
                ui.appendMessage("System", "Пользователь " + userName + " подключился\n", Color.GREEN);
            } else if (name.equals("userlogout")) {
                ui.appendMessage("System", "Пользователь " + userName + " отключился\n", Color.RED);
            }
        } else if (root.getTagName().equals("error")) {
            NodeList messageNodes = root.getElementsByTagName("message");
            if (messageNodes.getLength() > 0 && messageNodes.item(0) != null) {
                ui.appendMessage("System", "Ошибка: " + messageNodes.item(0).getTextContent() + "\n", Color.RED);
            }
        }
    }
}