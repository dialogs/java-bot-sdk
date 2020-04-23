package im.dlg.botsdk.internal;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static dialog.SequenceAndUpdatesOuterClass.*;
import static im.dlg.botsdk.internal.InternalBot.RECONNECT_DELAY;

public class InternalBotStream implements StreamObserver<SeqUpdateBox> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalBot.class);

    private final InternalBot internalBot;
    private final AtomicInteger currentSequence;

    public InternalBotStream(InternalBot internalBot, AtomicInteger currentSequence) {
        this.internalBot = internalBot;
        this.currentSequence = currentSequence;
    }

    @Override
    public void onNext(SeqUpdateBox value) {
        UpdateSeqUpdate seqUpdate;

        try {
            seqUpdate = UpdateSeqUpdate.parseFrom(value.getUpdate().getValue());
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("Failed to parse incoming sequence update", e);
            return;
        }

        int newSequence = seqUpdate.getSeq();

        if (newSequence <= currentSequence.get()) {
            return;
        }

        internalBot.callListeners(seqUpdate);
        currentSequence.set(newSequence);
    }

    @Override
    public void onError(Throwable t) {
        LOGGER.error("onError", t);

        // TODO: Remove sleep.
        try {
            Thread.sleep(RECONNECT_DELAY);
        } catch (InterruptedException e) {
            LOGGER.error("onError.sleep", e);
        }

        internalBot.reconnect();
    }

    @Override
    public void onCompleted() {
        LOGGER.info("onCompleted");

        // TODO: Remove sleep.
        try {
            Thread.sleep(RECONNECT_DELAY);
        } catch (InterruptedException e) {
            LOGGER.error("onCompleted.sleep", e);
        }

        internalBot.reconnect();
    }

}