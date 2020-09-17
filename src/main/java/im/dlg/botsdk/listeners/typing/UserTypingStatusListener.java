package im.dlg.botsdk.listeners.typing;

import im.dlg.botsdk.model.TypingType;

public interface UserTypingStatusListener {

    void onUserTyping(int userId, TypingType typingType);

    void onUserStopTyping(int userId);

}
