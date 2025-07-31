package common.msg;

public class EventMessage extends Message {
    private String userName;

    public EventMessage(String event, String userName) {
        super(event);
        this.userName = userName;
    }

    public String getUserName() { return userName; }
}