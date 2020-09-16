package im.dlg.botsdk.api;

import com.google.common.collect.Sets;
import im.dlg.grpc.services.*;
import im.dlg.botsdk.internal.InternalBot;
import im.dlg.botsdk.model.Peer;
import im.dlg.botsdk.model.User;
import im.dlg.botsdk.utils.Constants;
import im.dlg.botsdk.utils.PeerUtils;
import io.grpc.ManagedChannel;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static im.dlg.grpc.services.UsersOuterClass.FullUser;
import static im.dlg.grpc.services.UsersOuterClass.RequestLoadFullUsers;

/**
 * Api to load user info
 */
public class UsersApi {

    private final ManagedChannel channel;
    private final InternalBot internalBot;
    private final PeersApi peersApi;

    public UsersApi(ManagedChannel channel, InternalBot internalBot, PeersApi peersApi) {
        this.channel = channel;
        this.internalBot = internalBot;
        this.peersApi = peersApi;
    }

    /**
     * Retrieves user info for a peer
     *
     * @param outPeer - user peer
     * @return future with the user data
     */
    public CompletableFuture<Optional<User>> get(Peer outPeer) {
        return get(Sets.newHashSet(outPeer)).thenApply(users ->
                users.stream().filter(u -> u.getPeer().getId() == outPeer.getId()).findFirst());
    }

    /**
     * Retrieves user info for several peers
     *
     * @param peers - set of peers
     * @return future with users data
     */
    public CompletableFuture<List<User>> get(Collection<Peer> peers) {
        Set<Peers.UserOutPeer> userOutPeers = new HashSet<>();
        Map<Integer, Peer> peerMap = new HashMap<>();

        for (Peer peer : peers) {
            userOutPeers.add(PeerUtils.toUserOutPeer(PeerUtils.toServerOutPeer(peer)));
            peerMap.put(peer.getId(), peer);
        }

        return internalBot.getRefEntities(userOutPeers, null).thenCompose(res -> {
            Map<Integer, UsersOuterClass.User> users = new HashMap<>();
            res.getUsersList().forEach(u -> users.put(u.getId(), u));

            return internalBot.withToken(
                    UsersGrpc.newFutureStub(channel),
                    stub -> stub.loadFullUsers(RequestLoadFullUsers.newBuilder()
                            .addAllUserPeers(userOutPeers)
                            .build()))
                    .thenApply(r -> r.getFullUsersList().stream().map(fu -> {
                        UsersOuterClass.User u = users.get(fu.getId());
                        String preferredLanguages = fu.getPreferredLanguagesCount() > 0 ? fu.getPreferredLanguages(0) : null;

                return new User(peerMap.get(fu.getId()), u.getData().getName(), u.getData().getNick().getValue(),
                        User.Sex.fromServerModel(u.getData().getSex()), fu.getAbout().getValue(),
                        preferredLanguages, fu.getTimeZone().getValue(),
                        fu.getCustomProfile()
                );
            }).collect(Collectors.toList()));
        });
    }

    /**
     * Return user's peer by id
     *
     * @param userId - user's id
     * @return future with the user's peer
     */
    public CompletableFuture<Optional<Peer>> findUserPeer(Integer userId) {
        return internalBot.findUserOutPeer(userId)
                .thenApply(u -> u.map(PeerUtils::toDomainPeer));
    }

    /**
     * Return list users for a substring of nick's (not complete coincidence!)
     *
     * @param query - user's nick substring
     * @return future with the users list
     */
    public CompletableFuture<List<User>> searchUsersByNickSubstring(String query) {
        SearchOuterClass.RequestPeerSearch request = SearchOuterClass.RequestPeerSearch.newBuilder()
                .addQuery(SearchOuterClass.SearchCondition.newBuilder()
                        .setSearchPeerTypeCondition(SearchOuterClass.SearchPeerTypeCondition.newBuilder()
                                .setPeerTypeValue(SearchOuterClass.SearchPeerType.SEARCHPEERTYPE_CONTACTS_VALUE)
                                .build()
                        )
                )
                .addQuery(SearchOuterClass.SearchCondition.newBuilder()
                        .setSearchPieceText(SearchOuterClass.SearchPieceText.newBuilder()
                                .setQuery(query)
                                .build()
                        )
                )
                .addAllOptimizations(Constants.OPTIMISATIONS)
                .build();

        return internalBot.withToken(
                SearchGrpc.newFutureStub(channel),
                stub -> stub.peerSearch(request))
                .thenCompose(t -> get(t.getUserPeersList().stream()
                        .map(peer -> PeerUtils.toDomainPeer(PeerUtils.toOutPeer(peer)))
                        .collect(Collectors.toList())))
                .thenApply(users -> users.stream().filter(u -> u.getNick().contains(query))
                        .collect(Collectors.toList()));
    }

    /**
     * Find in contacts by nick
     *
     * @param nick - user's nick
     * @return future with the user data
     */
    public CompletableFuture<List<User>> findContactsByNick(String nick) {
        ContactsOuterClass.RequestSearchContacts request = ContactsOuterClass.RequestSearchContacts.newBuilder()
                .setRequest(nick)
                .addAllOptimizations(Constants.OPTIMISATIONS)
                .build();

        return internalBot.withToken(
                ContactsGrpc.newFutureStub(channel),
                stub -> stub.searchContacts(request)
        ).thenCompose(t -> get(t.getUserPeersList().stream()
                .map(peer -> PeerUtils.toDomainPeer(PeerUtils.toOutPeer(peer)))
                .collect(Collectors.toList())));
    }

    /**
     * Return user's OutPeer for a nick
     *
     * @param nick - user's nick
     * @return future with the user OutPeer
     */
    public CompletableFuture<Optional<Peers.OutPeer>> findUserOutPeerByNick(String nick) {
        return peersApi.resolvePeer(nick)
                .thenApply(Peer::getId)
                .thenCompose(internalBot::findUserOutPeer);
    }

    /**
     * Return user's full info for a nick
     *
     * @param nick - user's nick
     * @return future with the user FullInfo
     */
    public CompletableFuture<Optional<FullUser>> getUserFullProfileByNick(String nick) {
        return peersApi.resolvePeer(nick)
                .thenCompose(this::getUserFullProfileByPeer);
    }

    /**
     * Return user's full info for a nick
     *
     * @param peer - user's peer
     * @return future with the user FullInfo
     */
    public CompletableFuture<Optional<FullUser>> getUserFullProfileByPeer(Peer peer) {
        Peers.UserOutPeer outPeer = Peers.UserOutPeer.newBuilder()
                .setAccessHash(peer.getAccessHash())
                .setUid(peer.getId())
                .build();

        if (outPeer.getAccessHash() == 0 && outPeer.getUid() == 0) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        RequestLoadFullUsers request = RequestLoadFullUsers.newBuilder()
                .addUserPeers(outPeer)
                .build();

        return internalBot.withToken(
                UsersGrpc.newFutureStub(channel),
                stub -> stub.loadFullUsers(request))
                .thenApply(res -> {
                    if (res.getFullUsersCount() > 0) {
                        return Optional.of(res.getFullUsers(0));
                    }

                    return Optional.empty();
        });
    }

    /**
     * Return user's custom profile for a nick
     *
     * @param nick - user's nick
     * @return CustomProfile
     */
    public CompletableFuture<Optional<String>> getUserCustomProfileByNick(String nick) {
        return getUserFullProfileByNick(nick)
                .thenApply(userOpt -> userOpt.map(FullUser::getCustomProfile));
    }

    /**
     * Return user's custom profile for a nick
     *
     * @param peer - user's peer
     * @return CustomProfile
     */
    public CompletableFuture<Optional<String>> getUserCustomProfileByPeer(Peer peer) {
        return getUserFullProfileByPeer(peer)
                .thenApply(userOpt -> userOpt.map(FullUser::getCustomProfile));
    }

}
