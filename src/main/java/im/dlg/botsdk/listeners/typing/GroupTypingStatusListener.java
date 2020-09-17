package im.dlg.botsdk.listeners.typing;

import im.dlg.botsdk.model.TypingType;

import java.util.Map;

public interface GroupTypingStatusListener {

    void onGroupTypingStatusUpdate(int groupId, Map<Integer, TypingType> typingUsers);

}
