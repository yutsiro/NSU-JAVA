package common.msg;

public class ChatMessage extends Message {
    private String sender;
    private String text;

    public ChatMessage(String sender, String text, String sessionId) {
        super("message");
        this.sender = sender;
        this.text = text;
    }

    public String getSender() { return sender; }
    public String getText() { return text; }
}