package im.dlg.botsdk.light;

import im.dlg.botsdk.domain.DeviceType;

import java.time.Instant;

public interface UserOnlineStatusListener {

    void onUserOnline(int userId, DeviceType deviceType, String deviceCategory, Instant time);

    void onUserOffline(int userId, DeviceType deviceType, String deviceCategory, Instant time);

}
