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
import im.dlg.botsdk.domain.content.Content;
import im.dlg.botsdk.domain.Message;
import im.dlg.botsdk.light.UpdateListener;
import im.dlg.botsdk.utils.Constants;
import im.dlg.botsdk.utils.PeerUtils;
import im.dlg.botsdk.utils.UUIDUtils;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

// TODO: extract StreamObserver to different object
class InternalBotApi implements StreamObserver<SequenceAndUpdatesOuterClass.SeqUpdateBox> {

    DialogExecutor executor;
    ChannelWrapper channel;
    BotConfig botConfig;

    private volatile Metadata metadata;
    private Map<Peers.Peer, Peers.OutPeer> outPeerMap = new ConcurrentHashMap<>();
    private static Integer appId = 11011;
    private Map<Class, List<UpdateListener>> subscribers = new ConcurrentHashMap<>();

    InternalBotApi(ChannelWrapper wrapper, DialogExecutor executor) {
        this.botConfig = wrapper.getBotConfig();
        this.executor = executor;
        this.channel = wrapper;
    }

    InternalBotApi(BotConfig botConfig, DialogExecutor executor) {
        this.botConfig = botConfig;
        this.executor = executor;
        this.channel = new NettyChannelWrapper(this.botConfig);
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
                System.out.println("Bot registered with token = " + res.getToken());
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

            System.out.println("Bot authorized with id = " + p.getLeft().getId());

            return null;
        });
    }

    private void reconnect() {
        channel.connect();
        withToken(metadata, SequenceAndUpdatesGrpc.newStub(channel.getChannel()), stub -> {
            stub.seqUpdates(Empty.newBuilder().build(), this);
            return new Object();
        });
    }

    <T extends AbstractStub<T>, R> CompletableFuture<R> withToken(T stub, Function<T, ListenableFuture<R>> f) {
        T newStub = MetadataUtils.attachHeaders(stub, metadata);
        return executor.convert(f.apply(newStub));
    }

    <T extends AbstractStub<T>, R> R withToken(Metadata meta, T stub, Function<T, R> f) {
        T newStub = MetadataUtils.attachHeaders(stub, meta);
        return f.apply(newStub);
    }

    CompletableFuture<Optional<Peers.OutPeer>> findOutPeer(Peers.Peer peer) {
        return outPeerMap.containsKey(peer) ?
                CompletableFuture.completedFuture(Optional.of(outPeerMap.get(peer))) :
                // implicitly load outPeers of loaded dialogs
                loadDialogs(Sets.newHashSet(peer)).thenApplyAsync(x ->
                        outPeerMap.containsKey(peer) ? Optional.of(outPeerMap.get(peer)) : Optional.empty()
                );
    }

    CompletableFuture<Optional<Peers.OutPeer>> loadSenderOutPeer(Integer senderId, Peers.OutPeer peer, long date) {
        Peers.Peer senderPeer = PeerUtils.toUserPeer(senderId);
        return outPeerMap.containsKey(senderPeer) ?
                CompletableFuture.completedFuture(Optional.of(outPeerMap.get(senderPeer))) :
                // implicitly load outPeers of loaded dialogs
                load(peer, date, 2).thenApplyAsync(x ->
                        outPeerMap.containsKey(senderPeer) ? Optional.of(outPeerMap.get(senderPeer)) : Optional.empty()
                );
    }

    CompletableFuture<Optional<Peers.OutPeer>> findUserOutPeer(Integer userId) {
        Peers.Peer userPeer = PeerUtils.toUserPeer(userId);
        return findOutPeer(userPeer);
    }

    private void putOutPeer(Peers.OutPeer outPeer) {
        outPeerMap.put(PeerUtils.toPeer(outPeer), outPeer);
    }

    private void putOutPeer(Peers.UserOutPeer outPeer) {
        outPeerMap.put(PeerUtils.toPeer(outPeer), PeerUtils.toOutPeer(outPeer));
    }

    private void putOutPeer(Peers.GroupOutPeer outPeer) {
        outPeerMap.put(PeerUtils.toPeer(outPeer), PeerUtils.toOutPeer(outPeer));
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

    public synchronized <T extends GeneratedMessageV3> void subscribeOn(Class<T> clazz, UpdateListener<T> listener) {
        if (!subscribers.containsKey(clazz)) {
            subscribers.put(clazz, new CopyOnWriteArrayList<>());
        }

        subscribers.get(clazz).add(listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onNext(SequenceAndUpdatesOuterClass.SeqUpdateBox value) {
        try {
            SequenceAndUpdatesOuterClass.UpdateSeqUpdate upd =
                    SequenceAndUpdatesOuterClass.UpdateSeqUpdate.parseFrom(value.getUpdate().getValue());

            Object updateRaw = null;

            try {
                Field f = upd.getClass().getDeclaredField("update_");
                f.setAccessible(true);
                updateRaw = f.get(upd);

            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
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

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
        reconnect();
    }

    @Override
    public void onCompleted() {
        System.out.println("onCompleted");
        reconnect();
    }
}
