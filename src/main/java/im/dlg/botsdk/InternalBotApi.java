package im.dlg.botsdk;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import dialog.*;
import dialog.MessagingOuterClass.Dialog;
import dialog.MessagingOuterClass.HistoryMessage;
import dialog.MessagingOuterClass.ListLoadMode;
import dialog.MessagingOuterClass.RequestLoadDialogs;
import im.dlg.botsdk.domain.Message;
import im.dlg.botsdk.domain.content.Content;
import im.dlg.botsdk.light.UpdateListener;
import im.dlg.botsdk.utils.*;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.asynchttpclient.AsyncHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static dialog.SequenceAndUpdatesOuterClass.*;

// TODO: extract StreamObserver to different object
public class InternalBotApi implements StreamObserver<SeqUpdateBox> {

    public static final long RECONNECT_DELAY = 1000;
    private static final Integer appId = 11011;
    private final Logger log = LoggerFactory.getLogger(InternalBotApi.class);

    DialogExecutor executor;
    ChannelWrapper channel;
    private BotConfig botConfig;
    private AsyncHttpClient httpClient;

    private volatile Metadata metadata;
    private Map<String, Peers.OutPeer> outPeerMap = new ConcurrentHashMap<>();
    private Map<Class, List<UpdateListener>> subscribers = new ConcurrentHashMap<>();
    private AtomicInteger seq = new AtomicInteger();
    private RetryOptions retryOptions;

    InternalBotApi(BotConfig botConfig, DialogExecutor executor, AsyncHttpClient httpClient) {
        this.botConfig = botConfig;
        this.executor = executor;
        this.channel = new ChannelWrapper(this.botConfig);
        this.httpClient = httpClient;
        this.retryOptions = botConfig.getRetryOptions();
    }

    CompletableFuture<Void> start() {

        channel.connect();
        String deviceTitle = "BotWithToken";
        CompletableFuture<Metadata> meta = new CompletableFuture<>();

        return executor.convert(
                RegistrationGrpc.newFutureStub(channel.getChannel()).registerDevice(
                        RegistrationOuterClass.RequestRegisterDevice.newBuilder()
                                .setAppId(appId)
                                .setAppTitle(this.botConfig.getBotName())
                                .setDeviceTitle(this.botConfig.getBotName())
                                .build()
                )
        ).whenCompleteAsync((res, t) -> {
            if (res != null) {
                Metadata header = new Metadata();
                Metadata.Key<String> key = Metadata.Key.of("x-auth-ticket", Metadata.ASCII_STRING_MARSHALLER);
                header.put(key, res.getToken());

                meta.complete(header);
                metadata = header;
                log.info("Bot registered with token = " + res.getToken());
            } else if (t != null) {
                meta.completeExceptionally(t);
            }
        }).thenComposeAsync(res -> meta).thenComposeAsync(m -> {


            if (botConfig.getCertPath() != null && botConfig.getCertPassword() != null) {
                return executor.convert(withToken(m,
                        AuthenticationGrpc.newFutureStub(channel.getChannel()),
                        stub -> stub.startAnonymousAuth(
                                AuthenticationOuterClass.RequestStartAnonymousAuth.newBuilder()
                                        .setApiKey("BotSdk")
                                        .setAppId(appId)
                                        .setDeviceTitle(deviceTitle)
                                        .addPreferredLanguages("RU")
                                        .setTimeZone(StringValue.newBuilder().setValue("+3").build())
                                        .build()
                        )
                )).thenApplyAsync(res -> new ImmutablePair<>(res.getUser(), m));
            } else if (botConfig.getToken() != null) {
                return executor.convert(withToken(m,
                        AuthenticationGrpc.newFutureStub(channel.getChannel()),
                        stub -> stub.startTokenAuth(
                                AuthenticationOuterClass.RequestStartTokenAuth.newBuilder()
                                        .setApiKey("BotSdk")
                                        .setAppId(appId)
                                        .setDeviceTitle(deviceTitle)
                                        .addPreferredLanguages("RU")
                                        .setTimeZone(StringValue.newBuilder().setValue("+3").build())
                                        .setToken(botConfig.getToken())
                                        .build()
                        )
                )).thenApplyAsync(res -> new ImmutablePair<>(res.getUser(), m));
            } else {
                return null;
            }
        }).thenApplyAsync(p -> {

            withToken(p.getRight(),
                    SequenceAndUpdatesGrpc.newStub(channel.getChannel()),
                    stub -> {
                        stub.seqUpdates(Empty.newBuilder().build(), this);
                        return new Object();
                    }
            );

            log.info("Bot authorized with id = " + p.getLeft().getId());

            return null;
        });
    }

    public void reconnect() {
        channel.connect();
        withToken(metadata, SequenceAndUpdatesGrpc.newStub(channel.getChannel()), stub -> {
            stub.seqUpdates(Empty.newBuilder().build(), this);
            return new Object();
        });
    }

    <T extends AbstractStub<T>, R> CompletableFuture<R> withToken(T stub, Function<T, ListenableFuture<R>> f) {
        T newStub = MetadataUtils.attachHeaders(stub, metadata);
        TaskManager<R> task = new TaskManager<R>(executor.convert(f.apply(newStub)), this.retryOptions);
        return task.scheduleTask(0);
    }

    <T extends AbstractStub<T>, R> R withToken(Metadata meta, T stub, Function<T, R> f) {
        T newStub = MetadataUtils.attachHeaders(stub, meta);
        return f.apply(newStub);
    }

    <T extends AbstractStub<T>, R> StreamObserver<R> withObserverToken(T stub, Function<T, StreamObserver<R>> f) {
        T newStub = MetadataUtils.attachHeaders(stub, metadata);
        return f.apply(newStub);
    }

    CompletableFuture<Optional<Peers.OutPeer>> findOutPeer(Peers.Peer peer) {
        String peerHash = PeerUtils.peerHasher(peer);
        return outPeerMap.containsKey(peerHash) ?
                CompletableFuture.completedFuture(Optional.of(outPeerMap.get(peerHash))) :
                // implicitly load outPeers of loaded dialogs
                loadDialogs(Sets.newHashSet(peer)).thenApplyAsync(x ->
                        outPeerMap.containsKey(peerHash) ? Optional.of(outPeerMap.get(peerHash)) : Optional.empty()
                );
    }

    CompletableFuture<Optional<Peers.OutPeer>> loadSenderOutPeer(Integer senderId, Peers.OutPeer peer, long date) {
        Peers.Peer senderPeer = PeerUtils.toUserPeer(senderId);
        String peerHash = PeerUtils.peerHasher(senderPeer);
        return outPeerMap.containsKey(peerHash) ?
                CompletableFuture.completedFuture(Optional.of(outPeerMap.get(peerHash))) :
                // implicitly load outPeers of loaded dialogs
                load(peer, date, 2).thenApplyAsync(x ->
                        outPeerMap.containsKey(peerHash) ? Optional.of(outPeerMap.get(peerHash)) : Optional.empty()
                );
    }

    CompletableFuture<Optional<Peers.OutPeer>> findUserOutPeer(Integer userId) {
        Peers.Peer userPeer = PeerUtils.toUserPeer(userId);
        return findOutPeer(userPeer);
    }

    CompletableFuture<ResponseGetReferencedEntitites> getRefEntities(Collection<Peers.UserOutPeer> userOutPeers, Collection<Peers.GroupOutPeer> groupOutPeers) {

        RequestGetReferencedEntitites.Builder requestBuilder = RequestGetReferencedEntitites.newBuilder();

        if (userOutPeers != null) {
            requestBuilder.addAllUsers(userOutPeers);
        }

        if (groupOutPeers != null) {
            requestBuilder.addAllGroups(groupOutPeers);
        }

        return withToken(SequenceAndUpdatesGrpc.newFutureStub(channel.getChannel()),
                stub -> stub.getReferencedEntitites(requestBuilder.build()));
    }

    private void putOutPeer(Peers.OutPeer outPeer) {
        outPeerMap.put(PeerUtils.peerHasher(PeerUtils.toPeer(outPeer)), outPeer);
    }

    private void putOutPeer(Peers.UserOutPeer outPeer) {
        outPeerMap.put(PeerUtils.peerHasher(PeerUtils.toPeer(outPeer)), PeerUtils.toOutPeer(outPeer));
    }

    private void putOutPeer(Peers.GroupOutPeer outPeer) {
        outPeerMap.put(PeerUtils.peerHasher(PeerUtils.toPeer(outPeer)), PeerUtils.toOutPeer(outPeer));
    }

    public CompletableFuture<List<Message>> load(Peers.OutPeer peer, long from, Integer limit) {
        return withToken(
                MessagingGrpc.newFutureStub(channel.getChannel()),
                stub -> stub.loadHistory(MessagingOuterClass.RequestLoadHistory.newBuilder()
                        .setDate(from)
                        .setLimit(limit)
                        .setLoadMode(ListLoadMode.LISTLOADMODE_FORWARD)
                        .addAllOptimizations(Constants.OPTIMISATIONS)
                        .setPeer(peer)
                        .build()
                )
        ).thenApplyAsync(res -> {
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
        }, executor.getExecutor());
    }

    CompletableFuture<List<Dialog>> loadDialogs(Set<Peers.Peer> peers) {
        RequestLoadDialogs request = RequestLoadDialogs.newBuilder()
                .setLimit(1)
                .addAllPeersToLoad(peers)
                .addAllOptimizations(Constants.OPTIMISATIONS)
                .build();

        return withToken(
                MessagingGrpc.newFutureStub(channel.getChannel()),
                stub -> stub.loadDialogs(request)
        ).thenApplyAsync(res -> {
            res.getGroupPeersList().forEach(this::putOutPeer);
            res.getUserPeersList().forEach(this::putOutPeer);

            return res.getDialogsList();
        }, executor.getExecutor());
    }


    synchronized <T extends GeneratedMessageV3> void subscribeOn(Class<T> clazz, UpdateListener<T> listener) {
        if (!subscribers.containsKey(clazz)) {
            subscribers.put(clazz, new CopyOnWriteArrayList<>());
        }

        subscribers.get(clazz).add(listener);
    }

    CompletableFuture<Integer> getDifference(int seq) {
        RequestGetDifference request = RequestGetDifference.newBuilder().setSeq(seq).build();

        return withToken(SequenceAndUpdatesGrpc.newFutureStub(channel.getChannel())
                .withDeadlineAfter(2, TimeUnit.MINUTES), stub -> stub.getDifference(request))
                .thenCompose(res -> {

                    for (UpdateSeqUpdate update : res.getUpdatesList()) {
                        callListeners(update);
                    }

                    int lastSeq = res.getSeq();
                    this.seq.set(lastSeq);

                    if (res.getNeedMore()) {
                        return getDifference(lastSeq);
                    } else {
                        return CompletableFuture.completedFuture(lastSeq);
                    }
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onNext(SeqUpdateBox value) {
        try {

            UpdateSeqUpdate upd =
                    UpdateSeqUpdate.parseFrom(value.getUpdate().getValue());

            int newSeq = upd.getSeq();
            if (newSeq <= this.seq.get()) {
                return;
            }

            callListeners(upd);
            this.seq.set(newSeq);

        } catch (InvalidProtocolBufferException e) {
            log.error("onNext", e);
        }
    }

    private void callListeners(UpdateSeqUpdate upd) {
        Object updateRaw = null;

        try {
            Field f = upd.getClass().getDeclaredField("update_");
            f.setAccessible(true);
            updateRaw = f.get(upd);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("callListeners", e);
        }

        if (!(updateRaw instanceof GeneratedMessageV3)) {
            return;
        }

        final GeneratedMessageV3 up = (GeneratedMessageV3) updateRaw;

        for (Map.Entry<Class, List<UpdateListener>> entry : subscribers.entrySet()) {
            if (updateRaw.getClass().isAssignableFrom(entry.getKey())) {
                entry.getValue().forEach(listener -> listener.onUpdate(up));
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        log.error("onError", t);
        try {
            Thread.sleep(RECONNECT_DELAY);
        } catch (InterruptedException e) {
            log.error("onError.sleep", e);
        }
        reconnect();
    }

    @Override
    public void onCompleted() {
        log.info("onCompleted");
        try {
            Thread.sleep(RECONNECT_DELAY);
        } catch (InterruptedException e) {
            log.error("onCompleted.sleep", e);
        }
        reconnect();
    }

    AtomicInteger getSeq() {
        return seq;
    }

    AsyncHttpClient getHttpClient() {
        return httpClient;
    }
}
