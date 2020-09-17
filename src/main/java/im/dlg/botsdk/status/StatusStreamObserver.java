package im.dlg.botsdk.status;

import im.dlg.botsdk.api.StatusApi;
import im.dlg.botsdk.internal.InternalBot;
import im.dlg.botsdk.listeners.online.GroupOnlineStatusListener;
import im.dlg.botsdk.listeners.online.UserOnlineStatusListener;
import im.dlg.botsdk.listeners.typing.GroupTypingStatusListener;
import im.dlg.botsdk.listeners.typing.UserTypingStatusListener;
import im.dlg.botsdk.model.DeviceType;
import im.dlg.botsdk.model.TypingType;
import im.dlg.grpc.services.PresenceOuterClass;
import im.dlg.grpc.services.PresenceOuterClass.UpdateGroupOnline;
import im.dlg.grpc.services.PresenceOuterClass.UpdateUserLastSeen;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import static im.dlg.botsdk.internal.InternalBot.RECONNECT_DELAY;
import static im.dlg.grpc.services.PresenceOuterClass.*;
import static im.dlg.grpc.services.SequenceAndUpdatesOuterClass.WeakUpdateBox;

public class StatusStreamObserver implements StreamObserver<WeakUpdateBox> {

    private final Logger logger = LoggerFactory.getLogger(StatusApi.class);
    private final StatusStreamListenerRegistry listenerRegistry;
    private final InternalBot internalBot;

    public StatusStreamObserver(InternalBot internalBot, StatusStreamListenerRegistry listenerRegistry) {
        this.internalBot = internalBot;
        this.listenerRegistry = listenerRegistry;
    }

    @Override
    public void onNext(WeakUpdateBox weakUpdate) {
        if (weakUpdate.hasUserLastSeen()) {
            UpdateUserLastSeen updateUserLastSeen = weakUpdate.getUserLastSeen();

            int userId = updateUserLastSeen.getUid();
            UserOnlineStatusListener listener = listenerRegistry.getUserOnlineListener(userId);

            if (listener != null) {
                DeviceType deviceType = DeviceType.fromGrpcType(updateUserLastSeen.getDeviceType());
                Instant lastSeenAt = Instant.ofEpochMilli(updateUserLastSeen.getLastSeenAt());
                Instant currentTime = Instant.ofEpochMilli(updateUserLastSeen.getCurrentTime());

                if (lastSeenAt.isBefore(currentTime)) {
                    listener.onUserOffline(userId, deviceType, lastSeenAt);
                } else {
                    listener.onUserOnline(userId, deviceType, lastSeenAt);
                }
            }
        }

        if (weakUpdate.hasGroupOnline()) {
            UpdateGroupOnline updateGroupOnline = weakUpdate.getGroupOnline();

            int groupId = updateGroupOnline.getGroupId();
            GroupOnlineStatusListener listener = listenerRegistry.getGroupOnlineListener(groupId);

            if (listener != null) {
                int onlineUsers = updateGroupOnline.getCount();

                listener.onGroupOnlineStatusUpdate(groupId, onlineUsers);
            }
        }

        if (weakUpdate.hasTyping()) {
            UpdateTyping updateTyping = weakUpdate.getTyping();

            int userId = updateTyping.getUserId();
            UserTypingStatusListener listener = listenerRegistry.getUserTypingListener(userId);

            if (listener != null) {
                TypingType typingType = TypingType.fromGrpcType(updateTyping.getTypingType());

                listener.onUserTyping(userId, typingType);
            }
        }

        if (weakUpdate.hasTypingStop()) {
            UpdateTypingStop updateTypingStop = weakUpdate.getTypingStop();

            int userId = updateTypingStop.getUserId();
            UserTypingStatusListener listener = listenerRegistry.getUserTypingListener(userId);

            if (listener != null) {
                listener.onUserStopTyping(userId);
            }
        }

        if (weakUpdate.hasGroupTyping()) {
            UpdateGroupTyping updateGroupTyping = weakUpdate.getGroupTyping();

            int groupId = updateGroupTyping.getGroupId();
            GroupTypingStatusListener listener = listenerRegistry.getGroupTypingListener(groupId);

            if (listener != null) {
                Map<Integer, TypingType> typingUsers = updateGroupTyping.getUsersTypingList()
                        .stream()
                        .collect(Collectors.toMap(
                                UpdateGroupTyping.UserTyping::getUserId,
                                e -> TypingType.fromGrpcType(e.getTypingType())
                        ));

                listener.onGroupTypingStatusUpdate(groupId, typingUsers);
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
