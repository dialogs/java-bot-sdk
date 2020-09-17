package im.dlg.botsdk.listeners.online;

import im.dlg.botsdk.model.DeviceType;

import java.time.Instant;

public interface UserOnlineStatusListener {

    void onUserOnline(int userId, DeviceType deviceType, Instant lastSeenAt);

    void onUserOffline(int userId, DeviceType deviceType, Instant lastSeenAt);

}
