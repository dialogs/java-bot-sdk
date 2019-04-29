package im.dlg.botsdk;

import im.dlg.botsdk.utils.NetUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;

import java.io.File;
import java.security.Security;
import java.util.concurrent.TimeUnit;

public class ChannelWrapper {

    private ManagedChannel channel;
    private BotConfig botConfig;

    public ChannelWrapper(BotConfig botConfig) {
        this.botConfig = botConfig;
    }

    public BotConfig getBotConfig() {
        return botConfig;
    }

    public synchronized void connect() {
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            if (channel != null) {
                channel.shutdownNow();
            }

            NettyChannelBuilder nettyChannelBuilder = (NettyChannelBuilder) ManagedChannelBuilder
                    .forAddress(botConfig.getHost(), botConfig.getPort());

            if (botConfig.getCertPath() != null && botConfig.getCertPassword() != null) {

                SslContext sslContext =
                        GrpcSslContexts.forClient()
                                .keyManager(NetUtils.createKeyFactory(new File(botConfig.getCertPath()),
                                        botConfig.getCertPassword()))
                                .build();

                nettyChannelBuilder = nettyChannelBuilder.sslContext(sslContext)
                        .idleTimeout(10, TimeUnit.SECONDS);
            }

            if (!botConfig.isSecure()) {
                nettyChannelBuilder.usePlaintext();
            }

            this.channel = nettyChannelBuilder.build();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public synchronized ManagedChannel getChannel() {
        return channel;
    }
}
