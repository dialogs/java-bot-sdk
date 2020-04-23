package im.dlg.botsdk.status;

import im.dlg.botsdk.listeners.GroupOnlineStatusListener;
import im.dlg.botsdk.listeners.UserOnlineStatusListener;

import java.util.HashMap;
import java.util.Map;

public class StatusStreamListenerRegistry {

    private final Map<Integer, UserOnlineStatusListener> userListeners = new HashMap<>();
    private final Map<Integer, GroupOnlineStatusListener> groupListeners = new HashMap<>();

    void setUserListener(int userId, UserOnlineStatusListener userOnlineStatusListener) {
        userListeners.put(userId, userOnlineStatusListener);
    }

    void setGroupListener(int groupId, GroupOnlineStatusListener groupOnlineStatusListener) {
        groupListeners.put(groupId, groupOnlineStatusListener);
    }

    void removeUserListener(int userId) {
        userListeners.remove(userId);
    }

    void removeGroupListener(int groupId) {
        groupListeners.remove(groupId);
    }

    UserOnlineStatusListener getUserListener(int userId) {
        return userListeners.get(userId);
    }

    GroupOnlineStatusListener getGroupListener(int groupId) {
        return groupListeners.get(groupId);
    }

}
