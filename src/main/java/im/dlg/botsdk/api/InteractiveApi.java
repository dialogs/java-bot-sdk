package im.dlg.botsdk.api;

import com.google.protobuf.StringValue;
import dialog.MessagingGrpc;
import dialog.MessagingOuterClass.*;
import im.dlg.botsdk.internal.InternalBot;
import im.dlg.botsdk.listeners.InteractiveEventListener;
import im.dlg.botsdk.model.InteractiveEvent;
import im.dlg.botsdk.model.Peer;
import im.dlg.botsdk.model.interactive.*;
import im.dlg.botsdk.utils.MsgUtils;
import im.dlg.botsdk.utils.PeerUtils;
import im.dlg.botsdk.utils.UUIDUtils;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Api class for all the interactive requests, subscriptions
 */
public class InteractiveApi {

    private final Logger log = LoggerFactory.getLogger(InteractiveApi.class);

    private final ManagedChannel channel;
    private final InternalBot internalBot;
    private InteractiveEventListener listener = null;

    public InteractiveApi(ManagedChannel channel, InternalBot internalBot) {
        this.channel = channel;
        this.internalBot = internalBot;

        internalBot.subscribeOn(UpdateInteractiveMediaEvent.class, evt -> {
            try {
                internalBot.findUserOutPeer(evt.getUid()).thenAccept(opt -> opt.ifPresent(outPeer -> {
                    onEventInternal(new InteractiveEvent(UUIDUtils.convert(evt.getMid()), evt.getId(),
                            evt.getValue(), PeerUtils.toDomainPeer(outPeer)));
                }));
            } catch (Throwable throwable) {
                log.error("Failed to subscribe", throwable);
            }
        });
    }

    /**
     * Subscribe on interactive events
     *
     * @param listener - listener callback
     */
    @SuppressWarnings("unused")
    public void onEvent(InteractiveEventListener listener) {
        this.listener = listener;
    }

    /**
     * Send an interactive action group to particular peer
     *
     * @param peer  - the address peer user/channel/group
     * @param group - group of interactive elements
     * @return - future with message UUID, that completes when deliver to server
     */
    @SuppressWarnings("unused")
    public CompletableFuture<UUID> send(@Nonnull Peer peer, @Nonnull InteractiveGroup group) {
        return send(peer, group, null);
    }

    /**
     * Update the interactive message, change elements
     *
     * @param uuid  - message UUID
     * @param group - new widgets group
     * @return - future with message UUID, that completes when deliver to server
     */
    @SuppressWarnings("unused")
    public CompletableFuture<UUID> update(@Nonnull UUID uuid, @Nonnull InteractiveGroup group) {
        return update(uuid, group, null);
    }

    /**
     * Send an interactive action group and text to particular peer
     *
     * @param peer  - the address peer user/channel/group
     * @param group - group of interactive elements
     * @param text - message text (may be null)
     * @return - future with message UUID, that completes when deliver to server
     */
    public CompletableFuture<UUID> send(@Nonnull Peer peer, @Nonnull InteractiveGroup group, @Nullable String text) {
        RequestSendMessage.Builder request = RequestSendMessage.newBuilder()
                .setDeduplicationId(MsgUtils.uniqueCurrentTimeMS())
                .setPeer(PeerUtils.toServerOutPeer(peer));

        if (text != null) {
            request.setMessage(buildMessageContent(group, text));
        } else {
            request.setMessage(buildMessageContent(group));
        }

        return internalBot.withToken(
                MessagingGrpc.newFutureStub(channel),
                stub -> stub.sendMessage(request.build()))
                .thenApply(resp -> UUIDUtils.convert(resp.getMessageId()));
    }

    /**
     * Update the interactive message, change elements
     *
     * @param uuid  - message UUID
     * @param group - new widgets group
     * @param text - message text (may be null)
     * @return - future with message UUID, that completes when deliver to server
     */
    @SuppressWarnings("unused")
    public CompletableFuture<UUID> update(@Nonnull UUID uuid, @Nonnull InteractiveGroup group, @Nullable String text) {
        RequestUpdateMessage.Builder request = RequestUpdateMessage.newBuilder()
                .setMid(UUIDUtils.convertToApi(uuid))
                .setLastEditedAt(Instant.now().toEpochMilli());

        if (text != null) {
            request.setUpdatedMessage(buildMessageContent(group, text));
        } else {
            request.setUpdatedMessage(buildMessageContent(group));
        }

        return internalBot.withToken(
                MessagingGrpc.newFutureStub(channel),
                stub -> stub.updateMessage(request.build()))
                .thenApply(resp -> UUIDUtils.convert(resp.getMid()));
    }

    private MessageContent buildMessageContent(InteractiveGroup group) {
        return buildMessageContent(group, null);
    }

    private MessageContent buildMessageContent(InteractiveGroup group, String text) {
        InteractiveMediaGroup.Builder apiMediaGroup = InteractiveMediaGroup.newBuilder();

        if (group.getTitle() != null && !group.getTitle().isEmpty()) {
            apiMediaGroup.setTitle(StringValue.of(group.getTitle()));
        }

        if (group.getDescription() != null && !group.getDescription().isEmpty()) {
            apiMediaGroup.setDescription(StringValue.of(group.getDescription()));
        }

        for (InteractiveAction action : group.getActions()) {
            InteractiveMedia.Builder apiMedia = InteractiveMedia.newBuilder()
                    .setId(action.getId())
                    .setStyle(buildStyle(action.getStyle()));

            InteractiveWidget widget = action.getWidget();

            if (widget instanceof InteractiveButton) {
                apiMedia.setWidget(buildButton((InteractiveButton) widget));
            } else if (widget instanceof InteractiveSelect) {
                apiMedia.setWidget(buildSelectMenu((InteractiveSelect) widget));
            }

            if (action.getConfirm() != null) {
                apiMedia.setConfirm(buildConfirm(action));
            }

            apiMediaGroup.addActions(apiMedia);
        }

        MessageMedia messageMedia = MessageMedia.newBuilder()
                .addActions(apiMediaGroup)
                .build();

        TextMessage.Builder textAndMedia = TextMessage.newBuilder().addMedia(messageMedia);
        if (text != null) {
            textAndMedia.setText(text);
        }

        return MessageContent.newBuilder()
                .setTextMessage(textAndMedia.build())
                .build();
    }

    private InteractiveMediaWidget buildButton(InteractiveButton button) {
        InteractiveMediaButton.Builder btn = InteractiveMediaButton.newBuilder()
                .setValue(button.getValue());

        if (button.getLabel() != null && !button.getLabel().isEmpty()) {
            btn.setLabel(StringValue.of(button.getLabel()));
        }

        return InteractiveMediaWidget.newBuilder().setInteractiveMediaButton(btn).build();
    }

    private InteractiveMediaWidget buildSelectMenu(InteractiveSelect select) {
        InteractiveMediaSelect.Builder apiSelect = InteractiveMediaSelect.newBuilder();

        for (InteractiveSelectOption selectOption : select.getOptions()) {
            InteractiveMediaSelectOption.Builder apiSelectOption = InteractiveMediaSelectOption.newBuilder();
            apiSelectOption.setValue(selectOption.getValue()).setLabel(selectOption.getLabel());
            apiSelect.addOptions(apiSelectOption);
        }

        if (select.getLabel() != null && !select.getLabel().isEmpty()) {
            apiSelect.setLabel(StringValue.of(select.getLabel()));
        }

        if (select.getDefaultValue() != null && !select.getDefaultValue().isEmpty()) {
            apiSelect.setDefaultValue(StringValue.of(select.getDefaultValue()));
        }

        return InteractiveMediaWidget.newBuilder().setInteractiveMediaSelect(apiSelect).build();
    }

    private InteractiveMediaConfirm.Builder buildConfirm(InteractiveAction action) {
        InteractiveMediaConfirm.Builder confirm = InteractiveMediaConfirm.newBuilder();

        if (action.getConfirm().getText() != null) {
            confirm.setText(StringValue.of(action.getConfirm().getText()));
        }

        if (action.getConfirm().getTitle() != null) {
            confirm.setTitle(StringValue.of(action.getConfirm().getTitle()));
        }

        if (action.getConfirm().getOk() != null) {
            confirm.setOk(StringValue.of(action.getConfirm().getOk()));
        }

        if (action.getConfirm().getDismiss() != null) {
            confirm.setDismiss(StringValue.of(action.getConfirm().getDismiss()));
        }
        return confirm;
    }

    private InteractiveMediaStyle buildStyle(InteractiveAction.Style style) {
        if (style == InteractiveAction.Style.DANGER) {
            return InteractiveMediaStyle.INTERACTIVEMEDIASTYLE_DANGER;
        } else if (style == InteractiveAction.Style.PRIMARY) {
            return InteractiveMediaStyle.INTERACTIVEMEDIASTYLE_PRIMARY;
        } else {
            return InteractiveMediaStyle.INTERACTIVEMEDIASTYLE_DEFAULT;
        }
    }

    private void onEventInternal(InteractiveEvent event) {
        if (listener != null) {
            listener.onEvent(event);
            return;
        }

        log.debug("Got an event");
    }
}
