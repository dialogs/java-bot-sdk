package im.dlg.botsdk;

import com.google.protobuf.StringValue;
import dialog.*;
import dialog.TypingAndOnlineOuterClass.RequestSetOnline;
import im.dlg.botsdk.domain.DeviceType;
import im.dlg.botsdk.status.StatusStream;
import im.dlg.botsdk.status.StatusStreamListenerRegistry;
import im.dlg.botsdk.status.StatusStreamObserver;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static dialog.ObsoleteOuterClass.*;

public class StatusApi {

    private final Logger logger = LoggerFactory.getLogger(StatusApi.class);

    private final InternalBotApi internalBotApi;
    private StatusStream statusStream;

    public StatusApi(InternalBotApi internalBotApi) {
        this.internalBotApi = internalBotApi;
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

        return internalBotApi.withToken(
                TypingAndOnlineGrpc.newFutureStub(internalBotApi.channel.getChannel()),
                stub -> stub.setOnline(request)).thenApply(t -> null);
    }

    public StatusStream openStream() {
        if (statusStream != null) {
            return statusStream;
        }

        StatusStreamListenerRegistry listenerRegistry = new StatusStreamListenerRegistry();
        StatusStreamObserver statusStreamObserver = new StatusStreamObserver(internalBotApi, listenerRegistry);

        StreamObserver<ObsoleteWeakUpdateCommand> outgoingCommandsObserver =
                internalBotApi.withObserverToken(ObsoleteGrpc.newStub(internalBotApi.channel.getChannel()),
                stub -> stub.weakUpdates(statusStreamObserver));

        return new StatusStream(listenerRegistry, outgoingCommandsObserver);
    }

}
