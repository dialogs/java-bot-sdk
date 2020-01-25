package im.dlg.botsdk.status;

import dialog.ObsoleteOuterClass.ObsoleteWeakUpdateCommand;
import im.dlg.botsdk.domain.Group;
import im.dlg.botsdk.domain.Peer;
import im.dlg.botsdk.domain.User;
import im.dlg.botsdk.light.GroupOnlineStatusListener;
import im.dlg.botsdk.light.UserOnlineStatusListener;
import im.dlg.botsdk.utils.PeerUtils;
import io.grpc.stub.StreamObserver;

import static dialog.ObsoleteOuterClass.ObsoletePeersList;

public class StatusStream {

    private final StreamObserver<ObsoleteWeakUpdateCommand> outgoingCommandsObserver;
    private final StatusStreamListenerRegistry listenerRegistry;

    public StatusStream(StatusStreamListenerRegistry listenerRegistry, StreamObserver<ObsoleteWeakUpdateCommand> outgoingCommandsObserver) {
        this.listenerRegistry = listenerRegistry;
        this.outgoingCommandsObserver = outgoingCommandsObserver;
    }

    public void subscribeToUserOnline(User user, UserOnlineStatusListener listener) {
        listenerRegistry.setUserListener(user.getPeer().getId(), listener);
        subscribeToPeerOnline(user.getPeer());
    }

    public void unsubscribeFromUserOnline(User user) {
        unsubscribeFromPeerOnline(user.getPeer());
        listenerRegistry.removeUserListener(user.getPeer().getId());
    }

    public void subscribeToGroupOnline(Group group, GroupOnlineStatusListener listener) {
        listenerRegistry.setGroupListener(group.getPeer().getId(), listener);
        subscribeToPeerOnline(group.getPeer());
    }

    public void unsubscribeFromGroupOnline(Group group) {
        unsubscribeFromPeerOnline(group.getPeer());
        listenerRegistry.removeGroupListener(group.getPeer().getId());
    }

    private void subscribeToPeerOnline(Peer peer) {
        ObsoletePeersList peersList = ObsoletePeersList.newBuilder()
                .addPeers(PeerUtils.toObsoletePeer(peer))
                .build();

        ObsoleteWeakUpdateCommand command = ObsoleteWeakUpdateCommand.newBuilder()
                .setSubscribeToOnlines(peersList)
                .build();

        outgoingCommandsObserver.onNext(command);
    }

    private void unsubscribeFromPeerOnline(Peer peer) {
        ObsoletePeersList peersList = ObsoletePeersList.newBuilder()
                .addPeers(PeerUtils.toObsoletePeer(peer))
                .build();

        ObsoleteWeakUpdateCommand command = ObsoleteWeakUpdateCommand.newBuilder()
                .setUnsubscribeFromOnlines(peersList)
                .build();

        outgoingCommandsObserver.onNext(command);
    }

}
