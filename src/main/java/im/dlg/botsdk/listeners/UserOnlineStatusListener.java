package im.dlg.botsdk.listeners;

import im.dlg.botsdk.model.DeviceType;

import java.time.Instant;

public interface UserOnlineStatusListener {

    void onUserOnline(int userId, DeviceType deviceType, String deviceCategory, Instant time);

    void onUserOffline(int userId, DeviceType deviceType, String deviceCategory, Instant time);

}
