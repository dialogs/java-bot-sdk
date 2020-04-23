package im.dlg.botsdk.model;

import java.util.UUID;

import im.dlg.botsdk.model.content.Content;

/**
 * Message entity for any content
 */
public class Message {

    private Peer peer;
    private Peer sender;
    private UUID messageId;
    private String text;
    private long date;
    private Content messageContent;

    public Message(Peer peer, Peer sender, UUID messageId, String text, long date, Content messageContent) {
        this.peer = peer;
        this.sender = sender;
        this.messageId = messageId;
        this.text = text;
        this.date = date;
        this.messageContent = messageContent;
    }

    /**
     * @return Chat peer of message
     */
    public Peer getPeer() {
        return peer;
    }

    /**
     * @return Sender peer
     */
    public Peer getSender() {
        return sender;
    }

    /**
     * @return Message unique ID
     */
    public UUID getMessageId() {
        return messageId;
    }

    /**
     * @return Text of message
     */
    public String getText() {
        return text;
    }

    /**
     * @return Message timestamp
     */
    public long getDate() {
        return date;
    }

    /**
     * @return Attachment content
     */
    public Content getMessageContent() {
        return messageContent;
    }

    @Override
    public String toString() {
        return "Message{" +
                "peer=" + peer +
                ", sender=" + sender +
                ", messageId=" + messageId +
                ", text='" + text + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}
