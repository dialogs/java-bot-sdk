package im.dlg.botsdk.api;

import com.google.protobuf.StringValue;
import dialog.GroupsGrpc;
import dialog.GroupsOuterClass;
import dialog.GroupsOuterClass.RequestCreateGroup;
import dialog.SearchGrpc;
import im.dlg.botsdk.internal.InternalBot;
import im.dlg.botsdk.model.Group;
import im.dlg.botsdk.model.GroupType;
import im.dlg.botsdk.model.Peer;
import im.dlg.botsdk.model.User;
import im.dlg.botsdk.utils.PeerUtils;
import io.grpc.ManagedChannel;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static dialog.GroupsOuterClass.RequestJoinGroupByPeer;
import static dialog.Peers.GroupOutPeer;
import static dialog.SearchOuterClass.*;

public class GroupsApi {

    private final ManagedChannel channel;
    private final InternalBot internalBot;

    public GroupsApi(ManagedChannel channel, InternalBot internalBot) {
        this.channel = channel;
        this.internalBot = internalBot;
    }

    public CompletableFuture<Group> createGroup(String title, String username) {
        return createGroup(title, username, Collections.emptyList());
    }

    public CompletableFuture<Group> createGroup(String title, String username, List<User> users) {
        RequestCreateGroup.Builder request = RequestCreateGroup.newBuilder();

        for (User user : users) {
            request.addUsers(PeerUtils.toUserOutPeer(PeerUtils.toServerOutPeer(user.getPeer())));
        }

        request.setGroupType(GroupsOuterClass.GroupType.GROUPTYPE_GROUP)
                .setTitle(title)
                .setUsername(StringValue.of(username))
                .setRid(LocalDateTime.now().getSecond());

        return internalBot.withToken(
                GroupsGrpc.newFutureStub(channel),
                stub -> stub.createGroup(request.build())
        ).thenApply(g -> {
            GroupsOuterClass.Group group = g.getGroup();

            return new Group(
                    group.getShortname().getValue(),
                    group.getTitle(),
                    new Peer(group.getId(), Peer.PeerType.GROUP, group.getAccessHash()),
                    GroupType.fromServer(group.getGroupType()));
        });
    }

    public CompletableFuture<List<Group>> searchGroupByShortname(String query) {
        RequestPeerSearch request = RequestPeerSearch.newBuilder()
                .addQuery(SearchCondition.newBuilder()
                        .setSearchPeerTypeCondition(SearchPeerTypeCondition.newBuilder()
                                .setPeerTypeValue(SearchPeerType.SEARCHPEERTYPE_GROUPS_VALUE)
                                .build()))
                .addQuery(SearchCondition.newBuilder()
                        .setSearchPieceText(SearchPieceText.newBuilder()
                                .setQuery(query)
                                .build()))
                .build();

        return internalBot.withToken(
                SearchGrpc.newFutureStub(channel),
                stub -> stub.peerSearch(request))
                .thenCompose(searchResults -> internalBot.getRefEntities(null, new HashSet<>(searchResults.getGroupPeersList())))
                .thenApply(res -> res.getGroupsList().stream().map(g -> {
                    String shortname = g.getData() != null ? g.getData().getShortname().getValue() : g.getShortname().getValue();
                    String title = g.getData() != null ? g.getData().getTitle() : g.getTitle();

                    return new Group(shortname, title, new Peer(g.getId(), Peer.PeerType.GROUP, g.getAccessHash()),
                            GroupType.fromServer(g.getGroupType()));
                }).collect(Collectors.toList()));
    }

    public CompletableFuture<Void> joinGroupByPeer(Group group) {
        GroupOutPeer groupOutPeer = PeerUtils.toGroupOutPeer(PeerUtils.toServerOutPeer(group.getPeer()));

        RequestJoinGroupByPeer request = RequestJoinGroupByPeer.newBuilder()
                .setPeer(groupOutPeer)
                .build();

        return internalBot.withToken(GroupsGrpc.newFutureStub(channel),
                stub -> stub.joinGroupByPeer(request))
                .thenApply(t -> null);
    }

}