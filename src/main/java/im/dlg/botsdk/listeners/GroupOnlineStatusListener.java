package im.dlg.botsdk.listeners;

import java.time.Instant;

public interface GroupOnlineStatusListener {

    void onGroupStatusUpdate(int groupId, int onlineUsers, Instant time);

}