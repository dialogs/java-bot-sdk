package im.dlg.botsdk.api;

import dialog.Peers;
import dialog.SearchGrpc;
import dialog.SearchOuterClass;
import im.dlg.botsdk.internal.InternalBot;
import im.dlg.botsdk.model.Peer;
import im.dlg.botsdk.utils.PeerUtils;
import io.grpc.ManagedChannel;

import java.util.concurrent.CompletableFuture;

public class PeersApi {

    private final ManagedChannel channel;
    private final InternalBot internalBot;

    public PeersApi(ManagedChannel channel, InternalBot internalBot) {
        this.channel = channel;
        this.internalBot = internalBot;
    }

    public CompletableFuture<Peer> resolvePeer(String shortname) {
        SearchOuterClass.RequestResolvePeer request = SearchOuterClass.RequestResolvePeer.newBuilder()
                .setShortname(shortname)
                .build();

        return internalBot.withToken(
                SearchGrpc.newFutureStub(channel),
                stub -> stub.resolvePeer(request)
        ).thenApply(res -> {
                    Peers.OutPeer peer = res.getPeer();

                    return new Peer(
                            peer.getId(),
                            PeerUtils.toDomainPeerType(peer.getType()),
                            peer.getAccessHash());
                });
    }
}
