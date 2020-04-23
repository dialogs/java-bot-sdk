package im.dlg.botsdk;

import com.google.protobuf.StringValue;
import dialog.ObsoleteGrpc;
import dialog.TypingAndOnlineGrpc;
import dialog.TypingAndOnlineOuterClass.RequestSetOnline;
import im.dlg.botsdk.internal.InternalBot;
import im.dlg.botsdk.model.DeviceType;
import im.dlg.botsdk.status.StatusStream;
import im.dlg.botsdk.status.StatusStreamListenerRegistry;
import im.dlg.botsdk.status.StatusStreamObserver;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static dialog.ObsoleteOuterClass.ObsoleteWeakUpdateCommand;

public class StatusApi {

    private final ManagedChannel channel;
    private final InternalBot internalBot;
    private StatusStream statusStream;

    public StatusApi(ManagedChannel channel, InternalBot internalBot) {
        this.channel = channel;
        this.internalBot = internalBot;
    }

    public CompletableFuture<Void> setOnline(DeviceType deviceType, String deviceCategory, Duration timeout) {
        return setOnlineStatus(true, deviceType, deviceCategory, timeout);
    }

    public CompletableFuture<Void> setOffline(DeviceType deviceType, String deviceCategory, Duration timeout) {
        return setOnlineStatus(false, deviceType, deviceCategory, timeout);
    }

    private CompletableFuture<Void> setOnlineStatus(boolean status, DeviceType deviceType, String deviceCategory, Duration timeout) {
        RequestSetOnline request = RequestSetOnline.newBuilder()
                .setIsOnline(status)
                .setDeviceType(deviceType.toGrpcType())
                .setDeviceCategory(StringValue.newBuilder().setValue(deviceCategory).build())
                .setTimeout(timeout.toMillis())
                .build();

        return internalBot.withToken(
                TypingAndOnlineGrpc.newFutureStub(channel),
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
