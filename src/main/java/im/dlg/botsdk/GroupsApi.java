package im.dlg.botsdk;

import com.google.protobuf.StringValue;
import dialog.*;
import im.dlg.botsdk.domain.Group;
import im.dlg.botsdk.domain.GroupType;
import im.dlg.botsdk.domain.Peer;
import im.dlg.botsdk.utils.PeerUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GroupsApi {

    private InternalBotApi privateBot;

    GroupsApi(InternalBotApi privateBot) {
        this.privateBot = privateBot;
    }

    public CompletableFuture<Group> createGroup(String title, String username) {
        GroupsOuterClass.RequestCreateGroup request = GroupsOuterClass.RequestCreateGroup.newBuilder()
                .setGroupType(GroupsOuterClass.GroupType.GROUPTYPE_GROUP)
                .setTitle(title)
                .setUsername(StringValue.of(username))
                .setRid(LocalDateTime.now().getSecond())
                .build();

        return privateBot.withToken(
                GroupsGrpc.newFutureStub(privateBot.channel.getChannel()),
                stub -> stub.createGroup(request)
        ).thenApplyAsync(g -> {
            GroupsOuterClass.Group group = g.getGroup();
            return new Group(
                    group.getShortname().getValue(),
                    group.getTitle(),
                    new Peer(group.getId(), Peer.PeerType.GROUP, group.getAccessHash()),
                    GroupType.fromServer(group.getGroupType()));
        }, privateBot.executor.getExecutor());
    }

    public CompletableFuture<Void> inviteUser(Peer group, Peer peer) {
        GroupsOuterClass.RequestInviteUser request = GroupsOuterClass.RequestInviteUser.newBuilder()
                .setGroupPeer(PeerUtils.toGroupOutPeer(group))
                .setUser(PeerUtils.toUserOutPeer(peer))
                .setRid(LocalDateTime.now().getSecond())
                .build();

        return privateBot.withToken(
                GroupsGrpc.newFutureStub(privateBot.channel.getChannel()),
                stub -> stub.inviteUser(request)
        ).thenApplyAsync(res -> null, privateBot.executor.getExecutor());
    }

    public CompletableFuture<Void> kickUser(Peer group, Peer peer) {
        GroupsOuterClass.RequestKickUser request = GroupsOuterClass.RequestKickUser.newBuilder()
                .setGroupPeer(PeerUtils.toGroupOutPeer(group))
                .setUser(PeerUtils.toUserOutPeer(peer))
                .setRid(LocalDateTime.now().getSecond())
                .build();

        return privateBot.withToken(
                GroupsGrpc.newFutureStub(privateBot.channel.getChannel()),
                stub -> stub.kickUser(request)
        ).thenApplyAsync(res -> null, privateBot.executor.getExecutor());
    }

    public CompletableFuture<Void> leaveGroup(Peer group) {
        GroupsOuterClass.RequestLeaveGroup request = GroupsOuterClass.RequestLeaveGroup.newBuilder()
                .setGroupPeer(PeerUtils.toGroupOutPeer(group))
                .setRid(LocalDateTime.now().getSecond())
                .build();

        return privateBot.withToken(
                GroupsGrpc.newFutureStub(privateBot.channel.getChannel()),
                stub -> stub.leaveGroup(request)
        ).thenApplyAsync(res -> null, privateBot.executor.getExecutor());
    }

    public CompletableFuture<List<Group>> searchGroupByShortname(String query) {
        SearchOuterClass.RequestPeerSearch request = SearchOuterClass.RequestPeerSearch.newBuilder()
                .addQuery(SearchOuterClass.SearchCondition.newBuilder()
                        .setSearchPeerTypeCondition(SearchOuterClass.SearchPeerTypeCondition.newBuilder()
                                .setPeerTypeValue(SearchOuterClass.SearchPeerType.SEARCHPEERTYPE_GROUPS_VALUE)
                                .build()
                        )
                )
                .addQuery(SearchOuterClass.SearchCondition.newBuilder()
                        .setSearchPieceText(SearchOuterClass.SearchPieceText.newBuilder()
                                .setQuery(query)
                                .build()
                        )
                )
                .addOptimizations(Miscellaneous.UpdateOptimization.UPDATEOPTIMIZATION_GROUPS_V2)
                .build();

        return privateBot.withToken(
                SearchGrpc.newFutureStub(privateBot.channel.getChannel()),
                stub -> stub.peerSearch(request)
        ).thenApplyAsync(res -> res.getGroupsList().stream().map(g ->
                new Group(
                        g.getShortname().getValue(),
                        g.getTitle(),
                        new Peer(
                                g.getId(),
                                Peer.PeerType.GROUP,
                                g.getAccessHash()
                        ),
                        GroupType.fromServer(g.getGroupType())
                )
        ).collect(Collectors.toList()), privateBot.executor.getExecutor());
    }
}