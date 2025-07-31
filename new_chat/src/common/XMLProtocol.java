package common;

import javax.xml.parsers.*;

import common.msg.*;
import common.msg.LoginMessage;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.Socket;

public class XMLProtocol implements IMessageProtocol {
    private DataOutputStream dataOut;
    private DataInputStream dataIn;

    public XMLProtocol(Socket socket) throws IOException {
        dataOut = new DataOutputStream(socket.getOutputStream());
        dataIn = new DataInputStream(socket.getInputStream());
    }

    @Override
    public void sendMessage(Object message) throws IOException {
        String xml;
        if (message instanceof Message) {
            xml = convertToXML((Message) message);
        } else if (message instanceof String) {
            xml = (String) message;
        } else {
            throw new IllegalArgumentException("Unsupported message type for XMLProtocol: " + message);
        }
        if (xml.isEmpty()) {
            throw new IOException("XML не может быть пустым для сообщения: " + message);
        }
        byte[] bytes = xml.getBytes();
        dataOut.writeInt(bytes.length);
        dataOut.write(bytes);
        dataOut.flush();
    }

    private String convertToXML(Message msg) {
        if (msg instanceof SuccessMessage) {
            SuccessMessage success = (SuccessMessage) msg;
            String result = "<success>" + (success.getSessionId() != null ? "<session>" + success.getSessionId() + "</session>" : "") + "</success>";
            return result;
        } else if (msg instanceof ErrorMessage) {
            String result = "<error><message>" + ((ErrorMessage) msg).getErrorText() + "</message></error>";
            return result;
        } else if (msg instanceof ChatMessage) {
            ChatMessage chat = (ChatMessage) msg;
            String result = "<event name=\"message\"><name>" + (chat.getSender() != null ? chat.getSender() : "Неизвестный клиент") + "</name><message>" + chat.getText() + "</message></event>";
            return result;
        } else if (msg instanceof EventMessage) {
            EventMessage event = (EventMessage) msg;
            String result = "<event name=\"" + event.getCommand() + "\"><name>" + (event.getUserName() != null ? event.getUserName() : "Неизвестный клиент") + "</name></event>";
            return result;
        } else if (msg instanceof ListResponse) {
            ListResponse list = (ListResponse) msg;
            StringBuilder xml = new StringBuilder("<success><listusers>");
            for (String user : list.getUsers()) {
                if (user != null) {
                    xml.append("<user><name>").append(user).append("</name><type>CHAT_CLIENT</type></user>");
                }
            }
            xml.append("</listusers></success>");
            String result = xml.toString();
            return result;
        } else if (msg instanceof LoginMessage) {
            LoginMessage login = (LoginMessage) msg;
            String result = "<event name=\"login\"><name>" + login.getName() + "</name><type>" + login.getType() + "</type></event>";
            return result;
        } else if (msg instanceof LogoutMessage) {
            String result = "<event name=\"logout\"/>";
            return result;
        }
        throw new IllegalArgumentException("Неизвестный тип сообщения: " + msg.getClass());
    }

    @Override
    public Object receiveMessage() throws Exception {
        try {
            int available = dataIn.available();
            int length = dataIn.readInt();
            if (length <= 0) {
                throw new IOException("Invalid message length: " + length);
            }
            byte[] buffer = new byte[length];
            int bytesRead = 0;
            while (bytesRead < length) {
                int read = dataIn.read(buffer, bytesRead, length - bytesRead);
                if (read == -1) {
                    throw new IOException("Конец потока при чтении XML");
                }
                bytesRead += read;
            }
            String xml = new String(buffer);
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xml)));
            return doc;
        } catch (IOException e) {
            throw new Exception("Ошибка чтения XML: " + e.getMessage());
        } catch (SAXException e) {
            throw new Exception("Ошибка парсинга XML: " + e.getMessage());
        }
    }

    @Override
    public Object processMessage(Object message) throws Exception {
        return message;
    }

    @Override
    public void close() throws IOException {
        dataIn.close();
        dataOut.close();
    }
}