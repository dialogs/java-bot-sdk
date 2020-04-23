package im.dlg.botsdk.model;

import java.util.Objects;

public class Peer {

    private int id;
    private PeerType type;
    private long accessHash;

    public Peer(int id, PeerType type, long accessHash) {
        this.id = id;
        this.type = type;
        this.accessHash = accessHash;
    }

    public int getId() {
        return id;
    }

    public PeerType getType() {
        return type;
    }

    public long getAccessHash() {
        return accessHash;
    }

    public enum PeerType {
        PRIVATE,
        GROUP,
        SIP,
        UNKNOWN
    }

    @Override
    public String toString() {
        return "Peer{" +
                "id=" + id +
                ", type=" + type +
                ", accessHash=" + accessHash +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return id == peer.id &&
                accessHash == peer.accessHash &&
                type == peer.type;
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, type, accessHash);
    }
}
