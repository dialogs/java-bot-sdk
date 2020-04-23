package im.dlg.botsdk.utils;

import dialog.MessagingOuterClass;
import im.dlg.botsdk.model.content.Content;
import im.dlg.botsdk.model.Message;

import java.util.concurrent.atomic.AtomicLong;

public class MsgUtils {
    private static final AtomicLong LAST_TIME_MS = new AtomicLong();

    public static long uniqueCurrentTimeMS() {
        long now = System.currentTimeMillis();
        while (true) {
            long lastTime = LAST_TIME_MS.get();
            if (lastTime >= now)
                now = lastTime + 1;
            if (LAST_TIME_MS.compareAndSet(lastTime, now))
                return now;
        }
    }

    public static Message toMessage(MessagingOuterClass.HistoryMessage hm) {
        return new Message(
                PeerUtils.toDomainPeer(hm.getSenderPeer()),
                PeerUtils.toDomainPeer(hm.getSenderPeer()),
                UUIDUtils.convert(hm.getMid()),
                hm.getMessage().getTextMessage().getText(), hm.getDate(),
                Content.fromMessage(hm.getMessage()));
    }
}
