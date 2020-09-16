package im.dlg.botsdk.api;

import im.dlg.botsdk.internal.InternalBot;
import im.dlg.botsdk.model.DeviceType;
import im.dlg.botsdk.status.StatusStream;
import im.dlg.botsdk.status.StatusStreamListenerRegistry;
import im.dlg.botsdk.status.StatusStreamObserver;
import im.dlg.grpc.services.ObsoleteGrpc;
import im.dlg.grpc.services.PresenceGrpc;
import im.dlg.grpc.services.PresenceOuterClass.RequestSetOnline;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static im.dlg.grpc.services.ObsoleteOuterClass.ObsoleteWeakUpdateCommand;

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

    public StatusStream openStream() {
        if (statusStream != null) {
            return statusStream;
        }

        StatusStreamListenerRegistry listenerRegistry = new StatusStreamListenerRegistry();
        StatusStreamObserver statusStreamObserver = new StatusStreamObserver(internalBot, listenerRegistry);

        StreamObserver<ObsoleteWeakUpdateCommand> outgoingCommandsObserver =
                internalBot.withObserverToken(ObsoleteGrpc.newStub(channel),
                        stub -> stub.weakUpdates(statusStreamObserver));

        return statusStream = new StatusStream(listenerRegistry, outgoingCommandsObserver);
    }

}
