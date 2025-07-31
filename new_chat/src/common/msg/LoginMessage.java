package common.msg;

public class LoginMessage extends Message {
    private String name;
    private String type;

    public LoginMessage(String name, String type) {
        super("login");
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public String getType() { return type; }
}