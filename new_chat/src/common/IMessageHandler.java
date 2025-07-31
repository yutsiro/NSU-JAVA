package common;

public interface IMessageHandler {
    void handle(MessageContext context) throws Exception;
}
