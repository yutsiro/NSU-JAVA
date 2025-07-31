package common;

import java.io.IOException;

//обработчик сообщений, выбирает протокол
public class MessageProcessor {
    private IMessageProtocol protocol;

    public MessageProcessor(IMessageProtocol protocol) {
        this.protocol = protocol;
    }

    public void sendMessage(Object message) throws IOException {
        protocol.sendMessage(message);
    }

    public Object receiveMessage() throws Exception {
        return protocol.receiveMessage();
    }

    public Object processMessage(Object message) throws Exception {
        return protocol.processMessage(message);
    }

    public void close() throws IOException {
        protocol.close();
    }
}