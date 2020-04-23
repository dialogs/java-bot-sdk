package im.dlg.botsdk.listeners;

import com.google.protobuf.GeneratedMessageV3;

@FunctionalInterface
public interface UpdateListener<T extends GeneratedMessageV3> {
    void onUpdate(T update);
}
