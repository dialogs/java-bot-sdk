package im.dlg.botsdk.internal;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.StringValue;
import dialog.*;
import dialog.MessagingOuterClass.Dialog;
import dialog.MessagingOuterClass.HistoryMessage;
import dialog.MessagingOuterClass.ListLoadMode;
import dialog.MessagingOuterClass.RequestLoadDialogs;
import im.dlg.botsdk.BotConfig;
import im.dlg.botsdk.model.Message;
import im.dlg.botsdk.model.content.Content;
import im.dlg.botsdk.listeners.UpdateListener;
import im.dlg.botsdk.retry.TaskManager;
import im.dlg.botsdk.utils.*;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static dialog.Peers.*;
import static dialog.RegistrationOuterClass.*;
import static dialog.SequenceAndUpdatesOuterClass.*;

public class InternalBot {

    public static final long RECONNECT_DELAY = 1000;
    private static final Integer APP_ID = 11011;

    private final Logger log = LoggerFactory.getLogger(InternalBot.class);

    private final Map<String, OutPeer> outPeerMap = new ConcurrentHashMap<>();
    private final Map<Class, List<UpdateListener>> subscribers = new ConcurrentHashMap<>();
    private final AtomicInteger currentSequence = new AtomicInteger();
    private final InternalBotStream stream = new InternalBotStream(this, currentSequence);

    private final ManagedChannel channel;
    private final BotConfig config;
    private final boolean anonymousAuth;

    private volatile Metadata metadata;

    public InternalBot(ManagedChannel channel, BotConfig config, boolean anonymousAuth) {
        this.channel = channel;
        this.config = config;
        this.anonymousAuth = anonymousAuth;
    }

    public CompletableFuture<Void> start() {
        CompletableFuture<Metadata> meta = new CompletableFuture<>();

        RequestRegisterDevice request = RequestRegisterDevice.newBuilder()
                .setAppId(APP_ID)
                .setAppTitle(config.getBotName())
                .setDeviceTitle(config.getBotName())
                .build();

        return FutureConverter.toCompletableFuture(
                RegistrationGrpc.newFutureStub(channel).registerDevice(request)
        ).whenComplete((res, t) -> {
            if (res == null) {
                meta.completeExceptionally(t);
                return;
            }

            Metadata header = new Metadata();
            Metadata.Key<String> key = Metadata.Key.of("x-auth-ticket", Metadata.ASCII_STRING_MARSHALLER);
            header.put(key, res.getToken());

            meta.complete(header);
            metadata = header;
            log.info("Bot registered with token = {}", res.getToken());

        }).thenCompose(res -> meta).thenCompose(m -> {
            if (anonymousAuth) {
                return FutureConverter.toCompletableFuture(withToken(m,
                        AuthenticationGrpc.newFutureStub(channel),
                        stub -> stub.startAnonymousAuth(
                                AuthenticationOuterClass.RequestStartAnonymousAuth.newBuilder()
                                        .setApiKey("BotSdk")
                                        .setAppId(APP_ID)
                                        .setDeviceTitle(config.getBotName())
                                        .addPreferredLanguages("RU")
                                        .setTimeZone(StringValue.newBuilder().setValue("+3").build())
                                        .build()
                        )
                )).thenApply(res -> new ImmutablePair<>(res.getUser(), m));
            } else if (config.getToken() != null) {
                return FutureConverter.toCompletableFuture(withToken(m,
                        AuthenticationGrpc.newFutureStub(channel),
                        stub -> stub.startTokenAuth(
                                AuthenticationOuterClass.RequestStartTokenAuth.newBuilder()
                                        .setApiKey("BotSdk")
                                        .setAppId(APP_ID)
                                        .setDeviceTitle(config.getBotName())
                                        .addPreferredLanguages("RU")
                                        .setTimeZone(StringValue.newBuilder().setValue("+3").build())
                                        .setToken(config.getToken())
                                        .build()
                        )
                )).thenApply(res -> new ImmutablePair<>(res.getUser(), m));
            } else {
                return null;
            }
        }).thenApply(p -> {
            withToken(p.getRight(),
                    SequenceAndUpdatesGrpc.newStub(channel),
                    stub -> {
                        stub.seqUpdates(Empty.newBuilder().build(), stream);
                        return null;
                    }
            );

            log.info("Bot authorized with id = {}", p.getLeft().getId());
            return null;
        });
    }

    public void reconnect() {
        withToken(metadata, SequenceAndUpdatesGrpc.newStub(channel), stub -> {
            stub.seqUpdates(Empty.newBuilder().build(), stream);
            return new Object();
        });
    }

    public <T extends AbstractStub<T>, R> CompletableFuture<R> withToken(T stub, Function<T, ListenableFuture<R>> f) {
        T newStub = MetadataUtils.attachHeaders(stub, metadata);
        TaskManager<R> task = new TaskManager<>(FutureConverter.toCompletableFuture(f.apply(newStub)), config.getRetryOptions());
        return task.scheduleTask(0);
    }

    public <T extends AbstractStub<T>, R> R withToken(Metadata meta, T stub, Function<T, R> f) {
        T newStub = MetadataUtils.attachHeaders(stub, meta);
        return f.apply(newStub);
    }

    public <T extends AbstractStub<T>, R> StreamObserver<R> withObserverToken(T stub, Function<T, StreamObserver<R>> f) {
        T newStub = MetadataUtils.attachHeaders(stub, metadata);
        return f.apply(newStub);
    }

    public CompletableFuture<Optional<OutPeer>> findOutPeer(Peer peer) {
        String peerHash = PeerUtils.peerHasher(peer);

        OutPeer outPeer = outPeerMap.get(peerHash);

        if (outPeer != null) {
            return CompletableFuture.completedFuture(Optional.of(outPeer));
        }

        // Implicitly load outPeers of loaded dialogs
        return loadDialogs(Sets.newHashSet(peer))
                .thenApply(x -> outPeerMap.containsKey(peerHash) ?
                Optional.of(outPeerMap.get(peerHash)) : Optional.empty());
    }

    public CompletableFuture<Optional<OutPeer>> loadSenderOutPeer(Integer senderId, OutPeer peer, long date) {
        Peer senderPeer = PeerUtils.toUserPeer(senderId);
        String peerHash = PeerUtils.peerHasher(senderPeer);
        return outPeerMap.containsKey(peerHash) ?
                CompletableFuture.completedFuture(Optional.of(outPeerMap.get(peerHash))) :
                // implicitly load outPeers of loaded dialogs
                load(peer, date, 2).thenApply(x ->
                        outPeerMap.containsKey(peerHash) ? Optional.of(outPeerMap.get(peerHash)) : Optional.empty()
                );
    }

    public CompletableFuture<Optional<OutPeer>> findUserOutPeer(int userId) {
        return findOutPeer(PeerUtils.toUserPeer(userId));
    }

    public CompletableFuture<ResponseGetReferencedEntitites> getRefEntities(Collection<UserOutPeer> userOutPeers,
                                                                            Collection<GroupOutPeer> groupOutPeers) {
        RequestGetReferencedEntitites.Builder requestBuilder = RequestGetReferencedEntitites.newBuilder();

        if (userOutPeers != null) {
            requestBuilder.addAllUsers(userOutPeers);
        }

        if (groupOutPeers != null) {
            requestBuilder.addAllGroups(groupOutPeers);
        }

        return withToken(SequenceAndUpdatesGrpc.newFutureStub(channel),
                stub -> stub.getReferencedEntitites(requestBuilder.build()));
    }

    private void putOutPeer(OutPeer outPeer) {
        outPeerMap.put(PeerUtils.peerHasher(PeerUtils.toPeer(outPeer)), outPeer);
    }

    private void putOutPeer(UserOutPeer outPeer) {
        outPeerMap.put(PeerUtils.peerHasher(PeerUtils.toPeer(outPeer)), PeerUtils.toOutPeer(outPeer));
    }

    private void putOutPeer(GroupOutPeer outPeer) {
        outPeerMap.put(PeerUtils.peerHasher(PeerUtils.toPeer(outPeer)), PeerUtils.toOutPeer(outPeer));
    }

    public CompletableFuture<List<Message>> load(OutPeer peer, long from, Integer limit) {
        return withToken(
                MessagingGrpc.newFutureStub(channel),
                stub -> stub.loadHistory(MessagingOuterClass.RequestLoadHistory.newBuilder()
                        .setDate(from)
                        .setLimit(limit)
                        .setLoadMode(ListLoadMode.LISTLOADMODE_FORWARD)
                        .addAllOptimizations(Constants.OPTIMISATIONS)
                        .setPeer(peer)
                        .build())
        ).thenApply(res -> {
            res.getGroupPeersList().forEach(this::putOutPeer);
            res.getUserPeersList().forEach(this::putOutPeer);

            List<HistoryMessage> historyList = res.getHistoryList();
            List<Message> messages = new ArrayList<>();

            for (HistoryMessage hm : historyList) {
                putOutPeer(hm.getSenderPeer());

                messages.add(new Message(
                        PeerUtils.toDomainPeer(peer),
                        PeerUtils.toDomainPeer(hm.getSenderPeer()),
                        UUIDUtils.convert(hm.getMid()),
                        hm.getMessage().getTextMessage().getText(), hm.getDate(),
                        Content.fromMessage(hm.getMessage())
                ));
            }

            return messages;
        });
    }

    CompletableFuture<List<Dialog>> loadDialogs(Set<Peer> peers) {
        RequestLoadDialogs request = RequestLoadDialogs.newBuilder()
                .setLimit(1)
                .addAllPeersToLoad(peers)
                .addAllOptimizations(Constants.OPTIMISATIONS)
                .build();

        return withToken(
                MessagingGrpc.newFutureStub(channel),
                stub -> stub.loadDialogs(request)
        ).thenApply(res -> {
            res.getGroupPeersList().forEach(this::putOutPeer);
            res.getUserPeersList().forEach(this::putOutPeer);

            return res.getDialogsList();
        });
    }

    public synchronized <T extends GeneratedMessageV3> void subscribeOn(Class<T> clazz, UpdateListener<T> listener) {
         subscribers.computeIfAbsent(clazz, s -> new CopyOnWriteArrayList<>());
         subscribers.get(clazz).add(listener);
    }

    public int getCurrentSequence() {
        return currentSequence.get();
    }

    public CompletableFuture<Integer> getDifference(int seq) {
        RequestGetDifference request = RequestGetDifference.newBuilder().setSeq(seq).build();

        return withToken(SequenceAndUpdatesGrpc.newFutureStub(channel)
                .withDeadlineAfter(2, TimeUnit.MINUTES), stub -> stub.getDifference(request))
                .thenCompose(res -> {

                    for (UpdateSeqUpdate update : res.getUpdatesList()) {
                        callListeners(update);
                    }

                    int lastSeq = res.getSeq();
                    this.currentSequence.set(lastSeq);

                    if (res.getNeedMore()) {
                        return getDifference(lastSeq);
                    } else {
                        return CompletableFuture.completedFuture(lastSeq);
                    }
                });
    }

    void callListeners(UpdateSeqUpdate upd) {
        Object updateRaw = null;

        try {
            Field f = upd.getClass().getDeclaredField("update_");
            f.setAccessible(true);
            updateRaw = f.get(upd);
        } catch (Exception e) {
            log.error("Failed to get sequence update field", e);
        }

        if (!(updateRaw instanceof GeneratedMessageV3)) {
            return;
        }

        GeneratedMessageV3 up = (GeneratedMessageV3) updateRaw;

        for (Map.Entry<Class, List<UpdateListener>> entry : subscribers.entrySet()) {
            if (updateRaw.getClass().isAssignableFrom(entry.getKey())) {
                entry.getValue().forEach(listener -> listener.onUpdate(up));
            }
        }
    }

}
