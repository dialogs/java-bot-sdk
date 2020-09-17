package im.dlg.botsdk.status;

import im.dlg.botsdk.listeners.online.GroupOnlineStatusListener;
import im.dlg.botsdk.listeners.online.UserOnlineStatusListener;
import im.dlg.botsdk.listeners.typing.GroupTypingStatusListener;
import im.dlg.botsdk.listeners.typing.UserTypingStatusListener;
import im.dlg.botsdk.model.Group;
import im.dlg.botsdk.model.Peer;
import im.dlg.botsdk.model.User;
import im.dlg.botsdk.utils.PeerUtils;
import io.grpc.stub.StreamObserver;

import static im.dlg.grpc.services.SequenceAndUpdatesOuterClass.PeersList;
import static im.dlg.grpc.services.SequenceAndUpdatesOuterClass.WeakUpdateCommand;
import static im.dlg.grpc.services.SequenceAndUpdatesOuterClass.WeakUpdateCommand.SubscribeToOnlines;
import static im.dlg.grpc.services.SequenceAndUpdatesOuterClass.WeakUpdateCommand.newBuilder;

public class StatusStream {

    private final StreamObserver<WeakUpdateCommand> outgoingCommandsObserver;
    private final StatusStreamListenerRegistry listenerRegistry;

    public StatusStream(StatusStreamListenerRegistry listenerRegistry, StreamObserver<WeakUpdateCommand> outgoingCommandsObserver) {
        this.listenerRegistry = listenerRegistry;
        this.outgoingCommandsObserver = outgoingCommandsObserver;
    }

    public void subscribeToUserOnline(User user, UserOnlineStatusListener listener) {
        listenerRegistry.setUserOnlineListener(user.getPeer().getId(), listener);
        subscribeToPeerOnline(user.getPeer());
    }

    public void unsubscribeFromUserOnline(User user) {
        unsubscribeFromPeerOnline(user.getPeer());
        listenerRegistry.removeUserOnlineListener(user.getPeer().getId());
    }

    public void subscribeToGroupOnline(Group group, GroupOnlineStatusListener listener) {
        listenerRegistry.setGroupOnlineListener(group.getPeer().getId(), listener);
        subscribeToPeerOnline(group.getPeer());
    }

    public void unsubscribeFromGroupOnline(Group group) {
        unsubscribeFromPeerOnline(group.getPeer());
        listenerRegistry.removeGroupOnlineListener(group.getPeer().getId());
    }

    public void subscribeToUserTyping(User user, UserTypingStatusListener listener) {
        listenerRegistry.setUserTypingListener(user.getPeer().getId(), listener);
        subscribeToPeerTyping(user.getPeer());
    }

    public void subscribeToGroupTyping(Group group, GroupTypingStatusListener listener) {
        listenerRegistry.setGroupTypingListener(group.getPeer().getId(), listener);
        subscribeToPeerTyping(group.getPeer());
    }

    public void unsubscribeFromUserTyping(User user) {
        unsubscribeFromPeerTyping(user.getPeer());
        listenerRegistry.removeUserTypingListener(user.getPeer().getId());
    }

    public void unsubscribeFromGroupTyping(Group group) {
        unsubscribeFromPeerTyping(group.getPeer());
        listenerRegistry.removeGroupTypingListener(group.getPeer().getId());
    }

    private void subscribeToPeerOnline(Peer peer) {
        PeersList peersList = PeersList.newBuilder()
                .addPeers(PeerUtils.toServerPeer(peer))
                .build();

        SubscribeToOnlines subscribeToOnlines = SubscribeToOnlines.newBuilder()
                .setPeers(peersList)
                .setSendUsersId(false)
                .build();

        WeakUpdateCommand command = newBuilder()
                .setSubscribeToOnlines(subscribeToOnlines)
                .build();

        outgoingCommandsObserver.onNext(command);
    }

    private void unsubscribeFromPeerOnline(Peer peer) {
        PeersList peersList = PeersList.newBuilder()
                .addPeers(PeerUtils.toServerPeer(peer))
                .build();

        WeakUpdateCommand command = newBuilder()
                .setUnsubscribeFromOnlines(peersList).build();

        outgoingCommandsObserver.onNext(command);
    }

    private void subscribeToPeerTyping(Peer peer) {
        PeersList peersList = PeersList.newBuilder()
                .addPeers(PeerUtils.toServerPeer(peer))
                .build();

        WeakUpdateCommand command = newBuilder()
                .setSubscribeToTypings(peersList).build();

        outgoingCommandsObserver.onNext(command);
    }

    private void unsubscribeFromPeerTyping(Peer peer) {
        PeersList peersList = PeersList.newBuilder()
                .addPeers(PeerUtils.toServerPeer(peer))
                .build();

        WeakUpdateCommand command = newBuilder()
                .setUnsubscribeFromTypings(peersList).build();

        outgoingCommandsObserver.onNext(command);
    }

}
