package common;

import java.io.IOException;

//интерфейс для обработки протоколов
public interface IMessageProtocol {
    void sendMessage(Object message) throws IOException;
    Object receiveMessage() throws Exception;
    Object processMessage(Object message) throws Exception; //возвращает обработанное сообщение
    void close() throws IOException;
}