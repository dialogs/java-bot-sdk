package im.dlg.botsdk.listeners;

import im.dlg.botsdk.model.InteractiveEvent;

@FunctionalInterface
public interface InteractiveEventListener {
    void onEvent(InteractiveEvent event);
}
