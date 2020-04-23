package im.dlg.botsdk.status;

import dialog.ObsoleteOuterClass;
import dialog.ObsoleteOuterClass.ObsoleteWeakUpdateBox.ObsoleteUpdateGroupOnline;
import dialog.ObsoleteOuterClass.ObsoleteWeakUpdateBox.ObsoleteUpdateUserLastSeen;
import im.dlg.botsdk.api.StatusApi;
import im.dlg.botsdk.internal.InternalBot;
import im.dlg.botsdk.listeners.GroupOnlineStatusListener;
import im.dlg.botsdk.listeners.UserOnlineStatusListener;
import im.dlg.botsdk.model.DeviceType;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import static im.dlg.botsdk.internal.InternalBot.RECONNECT_DELAY;

public class StatusStreamObserver implements StreamObserver<ObsoleteOuterClass.ObsoleteWeakUpdateBox> {

    private final Logger logger = LoggerFactory.getLogger(StatusApi.class);
    private final StatusStreamListenerRegistry listenerRegistry;
    private final InternalBot internalBot;

    public StatusStreamObserver(InternalBot internalBot, StatusStreamListenerRegistry listenerRegistry) {
        this.internalBot = internalBot;
        this.listenerRegistry = listenerRegistry;
    }

    @Override
    public void onNext(ObsoleteOuterClass.ObsoleteWeakUpdateBox weakUpdate) {
        if (weakUpdate.hasUserLastSeen()) {
            ObsoleteUpdateUserLastSeen updateUserLastSeen = weakUpdate.getUserLastSeen();

            int userId = updateUserLastSeen.getUserId();
            UserOnlineStatusListener listener = listenerRegistry.getUserListener(userId);

            if (listener != null) {
                DeviceType deviceType = DeviceType.fromRawInt(updateUserLastSeen.getDeviceType());
                String deviceCategory = updateUserLastSeen.getDeviceCategory();
                Instant time = Instant.ofEpochMilli(updateUserLastSeen.getEpochMillis());

                if (updateUserLastSeen.getIsOnline()) {
                    listener.onUserOnline(userId, deviceType, deviceCategory, time);
                } else {
                    listener.onUserOffline(userId, deviceType, deviceCategory, time);
                }
            }
        }

        if (weakUpdate.hasGroupOnline()) {
            ObsoleteUpdateGroupOnline updateGroupOnline = weakUpdate.getGroupOnline();

            int groupId = updateGroupOnline.getGroupId();
            GroupOnlineStatusListener listener = listenerRegistry.getGroupListener(groupId);

            if (listener != null) {
                int onlineUsers = updateGroupOnline.getCount();
                Instant time = Instant.ofEpochMilli(updateGroupOnline.getClock());

                listener.onGroupStatusUpdate(groupId, onlineUsers, time);
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        logger.error("Weak update stream caught exception", t);

        try {
            Thread.sleep(RECONNECT_DELAY);
        } catch (InterruptedException e) {
            logger.error("Weak update reconnect sleep was interrupted", e);
        }

        internalBot.reconnect();
    }

    @Override
    public void onCompleted() {
        logger.error("Weak update stream unexpectedly closed");

        try {
            Thread.sleep(RECONNECT_DELAY);
        } catch (InterruptedException e) {
            logger.error("Weak update reconnect sleep was interrupted", e);
        }

        internalBot.reconnect();
    }

}
