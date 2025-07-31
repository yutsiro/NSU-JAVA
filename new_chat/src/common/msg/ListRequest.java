package common.msg;

public class ListRequest extends Message {
    public ListRequest(String sessionId) {
        super("list");
    }

}