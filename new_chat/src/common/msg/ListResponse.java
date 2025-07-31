package common.msg;

public class ListResponse extends Message {
    private String[] users;

    public ListResponse(String[] users) {
        super("list");
        this.users = users;
    }

    public String[] getUsers() { return users; }
}
