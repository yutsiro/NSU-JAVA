package common;

import common.msg.Message;
import org.w3c.dom.Document;

public class MessageContext {
    private Message message; //для сериализации
    private Document xmlDocument; //для XML
    private String protocol;

    public MessageContext(Message message, String protocol) {
        this.message = message;
        this.protocol = protocol;
    }

    public MessageContext(Document xmlDocument, String protocol) {
        this.xmlDocument = xmlDocument;
        this.protocol = protocol;
    }

    public Message getMessage() { return message; }
    public Document getXmlDocument() { return xmlDocument; }
    public String getProtocol() { return protocol; }
}