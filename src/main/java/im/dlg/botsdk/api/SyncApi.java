package im.dlg.botsdk.api;

import im.dlg.botsdk.internal.InternalBot;

import java.util.concurrent.CompletableFuture;

public class SyncApi {

    private final InternalBot privateBot;

    public SyncApi(InternalBot privateBot) {
        this.privateBot = privateBot;
    }

    /**
     * Current application seq number
     *
     * @return seq
     */
    public int getCurrentSeq() {
        return privateBot.getCurrentSequence();
    }

    /**
     * Restore state and trigger events to all the callbacks
     *
     * @param seq
     * @return the future when all updates downloaded
     */
    public CompletableFuture<Integer> restoreFromSeq(int seq) {
        return privateBot.getDifference(seq);
    }
}
