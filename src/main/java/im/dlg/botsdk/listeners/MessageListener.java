package im.dlg.botsdk.listeners;

import im.dlg.botsdk.model.Message;

@FunctionalInterface
public interface MessageListener {
    void onMessage(Message message);
}
