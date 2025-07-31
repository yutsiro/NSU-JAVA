package common.msg;

public class SuccessMessage extends Message {
    private String sessionId;

    public SuccessMessage(String sessionId) {
        super("success");
        this.sessionId = sessionId;
    }

    public String getSessionId() { return sessionId; }
}