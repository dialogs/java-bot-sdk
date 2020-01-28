package im.dlg.botsdk;

import com.google.protobuf.StringValue;
import dialog.*;
import im.dlg.botsdk.domain.Group;
import im.dlg.botsdk.domain.GroupType;
import im.dlg.botsdk.domain.Peer;
import im.dlg.botsdk.domain.User;
import im.dlg.botsdk.utils.PeerUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static dialog.GroupsOuterClass.*;
import static dialog.Peers.*;
import static dialog.SearchOuterClass.*;

public class GroupsApi {

    private InternalBotApi privateBot;

    GroupsApi(InternalBotApi privateBot) {
        this.privateBot = privateBot;
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

        return privateBot.withToken(
                GroupsGrpc.newFutureStub(privateBot.channel.getChannel()),
                stub -> stub.createGroup(request.build())
        ).thenApplyAsync(g -> {
            GroupsOuterClass.Group group = g.getGroup();
            return new Group(
                    group.getShortname().getValue(),
                    group.getTitle(),
                    new Peer(group.getId(), Peer.PeerType.GROUP, group.getAccessHash()),
                    GroupType.fromServer(group.getGroupType()));
        }, privateBot.executor.getExecutor());
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

        return privateBot.withToken(
                SearchGrpc.newFutureStub(privateBot.channel.getChannel()),
                stub -> stub.peerSearch(request))
                .thenComposeAsync(searchResults -> privateBot.getRefEntities(null, searchResults.getGroupPeersList()), privateBot.executor.getExecutor())
                .thenApplyAsync(res -> res.getGroupsList().stream().map(g -> {

                            String shortname = g.getData() != null ? g.getData().getShortname().getValue()
                                    : g.getShortname().getValue();
                            String title = g.getData() != null ? g.getData().getTitle() : g.getTitle();

                            return new Group(shortname, title,
                                    new Peer(g.getId(), Peer.PeerType.GROUP, g.getAccessHash()),
                                    GroupType.fromServer(g.getGroupType()));


                        }
                ).collect(Collectors.toList()), privateBot.executor.getExecutor());
    }

    public CompletableFuture<Void> joinGroupByPeer(Group group) {
        GroupOutPeer groupOutPeer = PeerUtils.toGroupOutPeer(PeerUtils.toServerOutPeer(group.getPeer()));

        RequestJoinGroupByPeer request = RequestJoinGroupByPeer.newBuilder()
                .setPeer(groupOutPeer)
                .build();

        return privateBot.withToken(GroupsGrpc.newFutureStub(privateBot.channel.getChannel()),
                stub -> stub.joinGroupByPeer(request))
                .thenApply(t -> null);
    }

}