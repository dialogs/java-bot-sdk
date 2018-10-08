package im.dlg.botsdk.reminderbot;

import im.dlg.botsdk.domain.Peer;

import java.lang.Integer;

public class Reminder implements Comparable<Reminder> {
    private final Peer peer;
    private final String message;
    private final Long unixTime;

    public Reminder(Peer peer, String message, Long unixTime) {
        this.peer = peer;
        this.message = message;
        this.unixTime = unixTime;
    }

    public Peer getPeer() {
        return peer;
    }

    public String getMessage() {
        return message;
    }

    public Long getUnixTime() {
        return unixTime;
    }

    @Override
    public int compareTo(Reminder o) {
        int result = unixTime.compareTo(o.getUnixTime());

        if (result == 0) {
            result = message.compareTo(o.getMessage());
        }

        if (result == 0) {
            result = Integer.compare(peer.getId(), o.getPeer().getId());
        }

        return result;
    }
}
