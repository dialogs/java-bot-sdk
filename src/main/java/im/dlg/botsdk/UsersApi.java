package im.dlg.botsdk;

import com.google.common.collect.Sets;
import dialog.*;
import im.dlg.botsdk.domain.Peer;
import im.dlg.botsdk.domain.User;
import im.dlg.botsdk.utils.PeerUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Api to load user info
 */
public class UsersApi {

    private InternalBotApi privateBot;

    UsersApi(InternalBotApi privateBot) {
        this.privateBot = privateBot;
    }

    /**
     * Retrieves user info for a peer
     *
     * @param outPeer - user peer
     * @return future with the user data
     */
    public CompletableFuture<Optional<User>> get(Peer outPeer) {
        return get(Sets.newHashSet(outPeer)).thenApplyAsync(users ->
                users.stream().filter(u -> u.getPeer().getId() == outPeer.getId()).findFirst());
    }

    /**
     * Retrieves user info for several peers
     *
     * @param outPeers - set of peers
     * @return future with users data
     */
    public CompletableFuture<List<User>> get(Set<Peer> outPeers) {
        Set<Peers.UserOutPeer> userOutPeers = new HashSet<>();
        Map<Integer, Peer> peerMap = new HashMap<>();

        for (Peer peer : outPeers) {
            userOutPeers.add(PeerUtils.toUserOutPeer(PeerUtils.toServerOutPeer(peer)));
            peerMap.put(peer.getId(), peer);
        }

        return privateBot.withToken(
                SequenceAndUpdatesGrpc.newFutureStub(privateBot.channel.getChannel()),
                stub -> stub.getReferencedEntitites(
                        SequenceAndUpdatesOuterClass.RequestGetReferencedEntitites.newBuilder()
                                .addAllUsers(userOutPeers)
                                .build())

        ).thenComposeAsync(res -> {
            Map<Integer, UsersOuterClass.User> users = new HashMap<>();
            res.getUsersList().forEach(u -> users.put(u.getId(), u));

            return privateBot.withToken(
                    UsersGrpc.newFutureStub(privateBot.channel.getChannel()),
                    stub -> stub.loadFullUsers(UsersOuterClass.RequestLoadFullUsers.newBuilder()
                            .addAllUserPeers(userOutPeers)
                            .build()
                    )
            ).thenApplyAsync(r -> r.getFullUsersList().stream().map(fu -> {
                UsersOuterClass.User u = users.get(fu.getId());
                return new User(peerMap.get(fu.getId()), u.getData().getName(), u.getData().getNick().getValue(),
                        User.Sex.fromServerModel(u.getData().getSex()), fu.getAbout().getValue(),
                        fu.getPreferredLanguages(0), fu.getTimeZone().getValue(),
                        fu.getCustomProfile()
                );
            }).collect(Collectors.toList()), privateBot.executor.getExecutor());
        }, privateBot.executor.getExecutor());
    }

    public CompletableFuture<Optional<Peer>> findUserPeer(Integer userId) {
        return privateBot.findUserOutPeer(userId)
                .thenApply(u -> u.map(PeerUtils::toDomainPeer));
    }

    public CompletableFuture<List<User>> searchUserByNick(String nickname) {
        SearchOuterClass.RequestPeerSearch request = SearchOuterClass.RequestPeerSearch.newBuilder()
                .addQuery(SearchOuterClass.SearchCondition.newBuilder()
                        .setSearchPeerTypeCondition(SearchOuterClass.SearchPeerTypeCondition.newBuilder()
                                .setPeerTypeValue(SearchOuterClass.SearchPeerType.SEARCHPEERTYPE_CONTACTS_VALUE)
                                .build()
                        )
                )
                .addQuery(SearchOuterClass.SearchCondition.newBuilder()
                        .setSearchPieceText(SearchOuterClass.SearchPieceText.newBuilder()
                                .setQuery(nickname)
                                .build()
                        )
                )
                .addOptimizations(Miscellaneous.UpdateOptimization.UPDATEOPTIMIZATION_COMPACT_USERS)
                .build();

        return privateBot.withToken(
                SearchGrpc.newFutureStub(privateBot.channel.getChannel()),
                stub -> stub.peerSearch(request)
        ).thenApplyAsync(res -> res.getUsersList().stream().map(u ->
                 new User(
                        new Peer(
                                u.getId(),
                                Peer.PeerType.PRIVATE,
                                u.getAccessHash()
                        ),
                        u.getData().getName(),
                        u.getData().getNick().getValue(),
                        User.Sex.fromServerModel(u.getData().getSex()),
                        "",
                        "",
                        u.getData().getTimeZone(),
                        ""
                )
        ).collect(Collectors.toList()), privateBot.executor.getExecutor());
    }

    public CompletableFuture<Peer> resolvePeer(String shortname) {
        SearchOuterClass.RequestResolvePeer request = SearchOuterClass.RequestResolvePeer.newBuilder()
                .setShortname(shortname)
                .build();

        return privateBot.withToken(
                SearchGrpc.newFutureStub(privateBot.channel.getChannel()),
                stub -> stub.resolvePeer(request)
        ).thenApplyAsync(res -> new Peer(res.getPeer().getId(), Peer.PeerType.PRIVATE, res.getPeer().getAccessHash()),privateBot.executor.getExecutor());
    }

}
