package im.dlg.botsdk.api;

import im.dlg.botsdk.internal.InternalBot;
import im.dlg.botsdk.model.DeviceType;
import im.dlg.botsdk.model.Peer;
import im.dlg.botsdk.model.TypingType;
import im.dlg.botsdk.status.StatusStream;
import im.dlg.botsdk.status.StatusStreamListenerRegistry;
import im.dlg.botsdk.status.StatusStreamObserver;
import im.dlg.botsdk.utils.PeerUtils;
import im.dlg.grpc.services.PresenceGrpc;
import im.dlg.grpc.services.PresenceOuterClass.RequestSetOnline;
import im.dlg.grpc.services.PresenceOuterClass.RequestStartTyping;
import im.dlg.grpc.services.SequenceAndUpdatesGrpc;
import im.dlg.grpc.services.SequenceAndUpdatesOuterClass.WeakUpdateCommand;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static im.dlg.grpc.services.Peers.OutPeer;
import static im.dlg.grpc.services.PresenceOuterClass.RequestStopTyping;

public class StatusApi {

    private final ManagedChannel channel;
    private final InternalBot internalBot;
    private StatusStream statusStream;

    public StatusApi(ManagedChannel channel, InternalBot internalBot) {
        this.channel = channel;
        this.internalBot = internalBot;
    }

    public CompletableFuture<Void> setOnline(DeviceType deviceType, Duration timeout) {
        return setOnlineStatus(true, deviceType, timeout);
    }

    public CompletableFuture<Void> setOffline(DeviceType deviceType, Duration timeout) {
        return setOnlineStatus(false, deviceType, timeout);
    }

    private CompletableFuture<Void> setOnlineStatus(boolean status, DeviceType deviceType, Duration timeout) {
        RequestSetOnline request = RequestSetOnline.newBuilder()
                .setIsOnline(status)
                .setDeviceType(deviceType.toGrpcType())
                .setTimeout(timeout.toMillis())
                .build();

        return internalBot.withToken(
                PresenceGrpc.newFutureStub(channel),
                stub -> stub.setOnline(request)).thenApply(t -> null);
    }

    public CompletableFuture<Void> setTyping(Peer peer, TypingType typingType) {
        OutPeer outPeer = PeerUtils.toServerOutPeer(peer);

        RequestStartTyping request = RequestStartTyping.newBuilder()
                .setTypingType(typingType.toGrpcType())
                .setPeer(outPeer)
                .build();

        return internalBot.withToken(PresenceGrpc.newFutureStub(channel),
                stub -> stub.startTyping(request)).thenApply(t -> null);
    }

    public CompletableFuture<Void> stopTyping(Peer peer) {
        OutPeer outPeer = PeerUtils.toServerOutPeer(peer);

        RequestStopTyping request = RequestStopTyping.newBuilder()
                .setPeer(outPeer)
                .build();

        return internalBot.withToken(PresenceGrpc.newFutureStub(channel),
                stub -> stub.stopTyping(request)).thenApply(t -> null);
    }

    public StatusStream openStream() {
        if (statusStream != null) {
            return statusStream;
        }

        StatusStreamListenerRegistry listenerRegistry = new StatusStreamListenerRegistry();
        StatusStreamObserver statusStreamObserver = new StatusStreamObserver(internalBot, listenerRegistry);

        StreamObserver<WeakUpdateCommand> outgoingCommandsObserver =
                internalBot.withObserverToken(SequenceAndUpdatesGrpc.newStub(channel),
                        stub -> stub.weakUpdates(statusStreamObserver));

        return statusStream = new StatusStream(listenerRegistry, outgoingCommandsObserver);
    }

}
