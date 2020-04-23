package im.dlg.botsdk.model;

import java.util.UUID;

/**
 * Event that happens when user interact to button/select menu
 */
public class InteractiveEvent {

    private UUID mid;
    private String id;
    private String value;
    private Peer peer;

    public InteractiveEvent(UUID mid, String id, String value, Peer peer) {
        this.mid = mid;
        this.id = id;
        this.value = value;
        this.peer = peer;
    }

    /**
     * @return message id of source interactive element
     */
    public UUID getMid() {
        return mid;
    }

    /**
     * @return id of interactive widget
     */
    public String getId() {
        return id;
    }

    /**
     * @return the value of button/selection menu
     */
    public String getValue() {
        return value;
    }

    /**
     * @return peer entity of user, who interact
     */
    public Peer getPeer() {
        return peer;
    }

    @Override
    public String toString() {
        return "InteractiveEvent{" +
                "mid=" + mid +
                ", id='" + id + '\'' +
                ", value='" + value + '\'' +
                ", peer=" + peer +
                '}';
    }
}
