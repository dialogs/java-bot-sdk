package im.dlg.botsdk;

import im.dlg.botsdk.internal.InternalBot;
import im.dlg.botsdk.utils.NetUtils;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.security.Security;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.*;
import static org.asynchttpclient.Dsl.asyncHttpClient;

public class BotSystem {

    private final ManagedChannel channel;
    private final AsyncHttpClient asyncHttpClient;
    private final boolean anonymousAuth;

    private BotSystem(ManagedChannel channel, AsyncHttpClient asyncHttpClient, boolean anonymousAuth) {
        this.channel = channel;
        this.asyncHttpClient = asyncHttpClient;
        this.anonymousAuth = anonymousAuth;
    }

    public static BotSystem createSystem(BotSystemConfig config) throws Exception {
        AsyncHttpClient asyncHttpClient = createAsyncHttpClient(config);
        ManagedChannel channel = createChannel(config);
        boolean anonymousAuth = config.getCertPath() != null && config.getCertPassword() != null;

        return new BotSystem(channel, asyncHttpClient, anonymousAuth);
    }

    private static AsyncHttpClient createAsyncHttpClient(BotSystemConfig config) throws Exception {
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();

        if (config.getCertPath() != null && config.getCertPassword() != null) {
                File certFile = new File(config.getCertPath());

                SslContext sslContext = SslContextBuilder.forClient()
                        .keyManager(NetUtils.createKeyFactory(certFile, config.getCertPassword()))
                        .build();

                builder.setSslContext(sslContext);
        }

        return asyncHttpClient(builder);
    }

    private static ManagedChannel createChannel(BotSystemConfig config) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        NettyChannelBuilder nettyChannelBuilder = (NettyChannelBuilder) ManagedChannelBuilder
                .forAddress(config.getHost(), config.getPort())
                .idleTimeout(15, SECONDS)
                .keepAliveTime(30, SECONDS);

        if (config.getCertPath() != null && config.getCertPassword() != null) {
            File certFile = new File(config.getCertPath());

            SslContext sslContext = GrpcSslContexts.forClient()
                    .keyManager(NetUtils.createKeyFactory(certFile, config.getCertPassword()))
                    .build();

            nettyChannelBuilder.sslContext(sslContext);
        }

        if (!config.isSecure()) {
            nettyChannelBuilder.usePlaintext();
        }

        if (!config.isCompression()) {
            nettyChannelBuilder.decompressorRegistry(DecompressorRegistry.emptyInstance());
        }

        return nettyChannelBuilder.build();
    }

    public CompletableFuture<Bot> startBot(BotConfig config) {
        InternalBot internalBot = new InternalBot(channel, config, anonymousAuth);

        return internalBot.start()
                .thenApply(v -> new Bot(channel, internalBot, asyncHttpClient));
    }

    public CompletableFuture<Bot> startBot(String token) {
        BotConfig botConfig = BotConfig.Builder.newBuilder()
                .withToken(token)
                .build();

        return startBot(botConfig);
    }

}
