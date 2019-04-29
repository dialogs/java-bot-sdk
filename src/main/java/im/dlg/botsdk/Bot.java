package im.dlg.botsdk;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Bot {

    private BotConfig botConfig;

    private final Object stopLock = new Object();

    private InternalBotApi internalBotApi;
    private MessagingApi messagingApi;
    private UsersApi users;
    private InteractiveApi interactiveApi;
    private MediaAndFilesApi mediaAndFilesApi;
    private DialogExecutor executor;

    private volatile CompletableFuture<Bot> voidCompletableFuture;

    private Bot() {
        this.executor = new DialogExecutor(4);
    }

    private Bot(BotConfig botConfig) {
        this.botConfig = botConfig;
        this.executor = new DialogExecutor(4);
    }


    /**
     * Launches the bot with token auth, the remaining configuration should go from config file
     *
     * @param token the token, received from parent bot or admin console
     * @return Future that completes when bot authorised
     */
    public static CompletableFuture<Bot> start(String token) {
        Config config = ConfigFactory.load("dialog.conf").getConfig("dialog.botsdk");

        BotConfig botConfig = new BotConfig.Builder().withToken(token)
                .withHost(config.getString("host"))
                .withPort(config.getInt("port")).build();

        Bot instance = new Bot(botConfig);
        instance.internalBotApi = new InternalBotApi(botConfig, instance.executor);

        return getBotCompletableFuture(instance);
    }

    /**
     * Launches the bot with full config
     *
     * @param channelWrapper the constructed grpc channel with bot config object
     * @return Future that completes when bot authorised
     */
    public static CompletableFuture<Bot> start(ChannelWrapper channelWrapper) {
        Bot instance = new Bot();
        instance.internalBotApi = new InternalBotApi(channelWrapper, instance.executor);

        return getBotCompletableFuture(instance);
    }

    /**
     * Launches the bot with full config
     *
     * @param botConfig the constructed config object
     * @return Future that completes when bot authorised
     */
    public static CompletableFuture<Bot> start(BotConfig botConfig) {
        Bot instance = new Bot();
        instance.internalBotApi = new InternalBotApi(botConfig, instance.executor);

        return getBotCompletableFuture(instance);
    }

    private static CompletableFuture<Bot> getBotCompletableFuture(Bot instance) {
        instance.voidCompletableFuture = instance.internalBotApi.start().thenAccept(v -> {
            instance.runApis(instance.internalBotApi);
        }).thenCompose(aVoid -> CompletableFuture.completedFuture(instance));

        return instance.voidCompletableFuture;
    }

    private void runApis(InternalBotApi internalBotApi) {
        messagingApi = new MessagingApi(internalBotApi);
        users = new UsersApi(internalBotApi);
        interactiveApi = new InteractiveApi(internalBotApi);
        mediaAndFilesApi = new MediaAndFilesApi(internalBotApi);
    }

    /**
     * Method to block execution
     *
     * @throws InterruptedException in wrong case
     */
    public void await() throws InterruptedException {
        synchronized (stopLock) {
            stopLock.wait();
        }
    }

    /**
     * Stops the bot
     */
    public void stop() {
        synchronized (stopLock) {
            stopLock.notifyAll();
        }
    }

    private void lock() {
        try {
            voidCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the object to interact with messaging
     */
    public MessagingApi messaging() {
        lock();
        return messagingApi;
    }

    /**
     * @return the object to interact with users
     */
    public UsersApi users() {
        lock();
        return users;
    }

    /**
     * @return the object to interact with buttons/select menus
     */
    public InteractiveApi interactiveApi() {
        lock();
        return interactiveApi;
    }

    /**
     * @return the object to interact with different media files
     */
    public MediaAndFilesApi mediaAndFilesApi() {
        lock();
        return mediaAndFilesApi;
    }
}
