package common.msg;

import java.io.Serializable;

public abstract class Message implements Serializable {
    protected String command;

    public Message(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}