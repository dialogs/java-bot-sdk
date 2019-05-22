package im.dlg.botsdk;

import io.grpc.ManagedChannel;

/**
 * @author Alex Scrobot
 */
public interface ChannelWrapper {

    BotConfig getBotConfig();
    void connect();
    ManagedChannel getChannel();

}
