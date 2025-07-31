package common.msg;

public class ErrorMessage extends Message {
    private String errorText;

    public ErrorMessage(String errorText) {
        super("error");
        this.errorText = errorText;
    }

    public String getErrorText() { return errorText; }
}