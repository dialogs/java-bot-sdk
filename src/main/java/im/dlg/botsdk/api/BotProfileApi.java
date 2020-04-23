package im.dlg.botsdk.api;

import com.google.protobuf.StringValue;
import dialog.ProfileGrpc;
import im.dlg.botsdk.internal.InternalBot;
import io.grpc.ManagedChannel;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

import static dialog.ProfileOuterClass.RequestEditAbout;
import static dialog.ProfileOuterClass.RequestEditName;

public class BotProfileApi {

    private final ManagedChannel channel;
    private final InternalBot internalBot;

    public BotProfileApi(ManagedChannel channel, InternalBot internalBot) {
        this.channel = channel;
        this.internalBot = internalBot;
    }

    public CompletableFuture<Void> editName(@Nonnull String name) {
        RequestEditName request = RequestEditName.newBuilder()
                .setName(name)
                .build();

        return internalBot.withToken(
                ProfileGrpc.newFutureStub(channel),
                stub -> stub.editName(request))
                .thenApply(t -> null);
    }

    public CompletableFuture<Void> editAbout(@Nonnull String about) {
        RequestEditAbout request = RequestEditAbout.newBuilder()
                .setAbout(StringValue.of(about))
                .build();

        return internalBot.withToken(ProfileGrpc.newFutureStub(channel),
                stub -> stub.editAbout(request))
                .thenApply(t -> null);
    }
}
