package common;

import java.io.*;
import java.net.Socket;

//протокол для сериализации
public class SerializationProtocol implements IMessageProtocol {
    private ObjectOutputStream objOut;
    private ObjectInputStream objIn;

    public SerializationProtocol(Socket socket) throws IOException {
        objOut = new ObjectOutputStream(socket.getOutputStream());
        objIn = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void sendMessage(Object message) throws IOException {
        objOut.writeObject(message);
    }

    @Override
    public Object receiveMessage() throws Exception {
        return objIn.readObject();
    }

    @Override
    public Object processMessage(Object message) throws Exception {
        return message; //возвращаем сообщение как есть
    }

    @Override
    public void close() throws IOException {
        objIn.close();
        objOut.close();
    }
}
