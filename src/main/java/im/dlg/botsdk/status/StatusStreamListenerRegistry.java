package im.dlg.botsdk.status;

import im.dlg.botsdk.listeners.online.GroupOnlineStatusListener;
import im.dlg.botsdk.listeners.online.UserOnlineStatusListener;
import im.dlg.botsdk.listeners.typing.GroupTypingStatusListener;
import im.dlg.botsdk.listeners.typing.UserTypingStatusListener;

import java.util.HashMap;
import java.util.Map;

public class StatusStreamListenerRegistry {

    private final Map<Integer, UserOnlineStatusListener> userOnlineListeners = new HashMap<>();
    private final Map<Integer, GroupOnlineStatusListener> groupOnlineListeners = new HashMap<>();
    private final Map<Integer, UserTypingStatusListener> userTypingListeners = new HashMap<>();
    private final Map<Integer, GroupTypingStatusListener> groupTypingListeners = new HashMap<>();

    void setUserOnlineListener(int userId, UserOnlineStatusListener userOnlineStatusListener) {
        userOnlineListeners.put(userId, userOnlineStatusListener);
    }

    void setUserTypingListener(int userId, UserTypingStatusListener userTypingStatusListener) {
        userTypingListeners.put(userId, userTypingStatusListener);
    }

    void setGroupOnlineListener(int groupId, GroupOnlineStatusListener groupOnlineStatusListener) {
        groupOnlineListeners.put(groupId, groupOnlineStatusListener);
    }

    void setGroupTypingListener(int groupId, GroupTypingStatusListener groupTypingStatusListener) {
        groupTypingListeners.put(groupId, groupTypingStatusListener);
    }

    void removeUserOnlineListener(int userId) {
        userOnlineListeners.remove(userId);
    }

    void removeUserTypingListener(int userId) {
        userTypingListeners.remove(userId);
    }

    void removeGroupOnlineListener(int groupId) {
        groupOnlineListeners.remove(groupId);
    }

    void removeGroupTypingListener(int groupId) {
        groupTypingListeners.remove(groupId);
    }

    UserOnlineStatusListener getUserOnlineListener(int userId) {
        return userOnlineListeners.get(userId);
    }

    UserTypingStatusListener getUserTypingListener(int userId) {
        return userTypingListeners.get(userId);
    }

    GroupOnlineStatusListener getGroupOnlineListener(int groupId) {
        return groupOnlineListeners.get(groupId);
    }

    GroupTypingStatusListener getGroupTypingListener(int groupId) {
        return groupTypingListeners.get(groupId);
    }

}
