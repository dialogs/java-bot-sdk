package im.dlg.botsdk.utils;

import dialog.ObsoleteOuterClass;
import dialog.ObsoleteOuterClass.ObsoletePeer;
import dialog.ObsoleteOuterClass.ObsoletePeer.ObsoletePeerType;
import dialog.Peers;
import im.dlg.botsdk.domain.Peer;

import static im.dlg.botsdk.domain.Peer.*;

public class PeerUtils {
    public static Peers.Peer toPeer(Peers.OutPeer op) {
        return Peers.Peer.newBuilder()
                .setType(op.getType())
                .setId(op.getId())
                .build();
    }

    public static Peers.Peer toPeer(Peers.UserOutPeer uop) {
        return Peers.Peer.newBuilder()
                .setType(Peers.PeerType.PEERTYPE_PRIVATE)
                .setId(uop.getUid())
                .build();
    }

    public static Peers.Peer toPeer(Peers.GroupOutPeer uop) {
        return Peers.Peer.newBuilder()
                .setType(Peers.PeerType.PEERTYPE_GROUP)
                .setId(uop.getGroupId())
                .build();
    }

    public static Peers.OutPeer toOutPeer(Peers.UserOutPeer uop) {
        return Peers.OutPeer.newBuilder()
                .setType(Peers.PeerType.PEERTYPE_PRIVATE)
                .setId(uop.getUid())
                .setAccessHash(uop.getAccessHash())
                .build();
    }

    public static Peers.OutPeer toOutPeer(Peers.GroupOutPeer uop) {
        return Peers.OutPeer.newBuilder()
                .setType(Peers.PeerType.PEERTYPE_GROUP)
                .setId(uop.getGroupId())
                .setAccessHash(uop.getAccessHash())
                .build();
    }

    public static Peers.UserOutPeer toUserOutPeer(Peers.OutPeer outPeer) {
        return Peers.UserOutPeer.newBuilder()
                .setUid(outPeer.getId())
                .setAccessHash(outPeer.getAccessHash())
                .build();
    }

    public static Peers.GroupOutPeer toGroupOutPeer(Peers.OutPeer outPeer) {
        return Peers.GroupOutPeer.newBuilder()
                .setGroupId(outPeer.getId())
                .setAccessHash(outPeer.getAccessHash())
                .build();
    }

    public static Peers.Peer toUserPeer(Integer userId) {
        return Peers.Peer.newBuilder()
                .setType(Peers.PeerType.PEERTYPE_PRIVATE)
                .setId(userId)
                .build();
    }

    public static Peer toDomainPeer(Peers.OutPeer outPeer) {

        PeerType type;

        switch (outPeer.getType()) {
            case PEERTYPE_PRIVATE:
                type = PeerType.PRIVATE;
                break;
            case PEERTYPE_GROUP:
                type = PeerType.GROUP;
                break;
            case PEERTYPE_SIP:
                type = PeerType.SIP;
                break;
            case PEERTYPE_ENCRYPTEDPRIVATE:
            case PEERTYPE_UNKNOWN:
            case UNRECOGNIZED:
            default:
                type = PeerType.UNKNOWN;
                break;
        }

        return new Peer(outPeer.getId(), type, outPeer.getAccessHash());
    }

    public static Peers.OutPeer toServerOutPeer(Peer peer) {

        Peers.PeerType type;
        switch (peer.getType()) {

            case PRIVATE:
                type = Peers.PeerType.PEERTYPE_PRIVATE;
                break;
            case GROUP:
                type = Peers.PeerType.PEERTYPE_GROUP;
                break;
            case SIP:
                type = Peers.PeerType.PEERTYPE_SIP;
                break;
            default:
                type = Peers.PeerType.PEERTYPE_UNKNOWN;
                break;
        }

        return Peers.OutPeer.newBuilder()
                .setType(type)
                .setId(peer.getId())
                .setAccessHash(peer.getAccessHash())
                .build();
    }

    public static ObsoletePeer toObsoletePeer(Peer peer) {
        return ObsoletePeer.newBuilder()
                .setId(peer.getId())
                .setAccessHash(peer.getAccessHash())
                .setType(toObsoletePeerType(peer.getType()))
                .build();
    }

    public static ObsoletePeerType toObsoletePeerType(PeerType peerType) {
        switch (peerType) {
            case PRIVATE:
                return ObsoletePeerType.OBSOLETE_PEERTYPE_PRIVATE;
            case GROUP:
                return ObsoletePeerType.OBSOLETE_PEERTYPE_GROUP;
            case SIP:
                return ObsoletePeerType.OBSOLETE_PEERTYPE_SIP;
            case UNKNOWN:
                return ObsoletePeerType.OBSOLETE_PEERTYPE_UNKNOWN;
            default:
                throw new IllegalStateException("Unsupported peer type");
        }
    }

    public static PeerType toDomainPeerType(Peers.PeerType peerType) {
        PeerType result = PeerType.UNKNOWN;
        switch (peerType) {
            case PEERTYPE_GROUP: result = PeerType.GROUP;
                break;

            case PEERTYPE_PRIVATE: result = PeerType.PRIVATE;
                break;

            case PEERTYPE_SIP: result = PeerType.SIP;
                break;

            case PEERTYPE_UNKNOWN: result = PeerType.UNKNOWN;
                break;
        }
        return result;
    }

    public static String peerHasher(Peers.Peer peer) {
        return peer.getId() + "_" + peer.getType().toString();
    }

    public static String peerHasher(Peers.OutPeer outPeer) {
        return outPeer.getId() + "_" + outPeer.getType().toString();
    }

    public static String peerHasher(Peers.UserOutPeer outPeer) {
        return outPeer.getUid() + "_" + Peers.PeerType.PEERTYPE_PRIVATE.toString();
    }
}
