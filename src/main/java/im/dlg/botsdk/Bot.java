package im.dlg.botsdk;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import im.dlg.botsdk.utils.NetUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class Bot {

    private static final Logger log = LoggerFactory.getLogger(Bot.class);

    private BotConfig botConfig;

    private final Object stopLock = new Object();

    private InternalBotApi internalBotApi;
    private MessagingApi messagingApi;
    private UsersApi users;
    private InteractiveApi interactiveApi;
    private GroupsApi groupsApi;
    private MediaAndFilesApi mediaAndFilesApi;
    private PeersApi peersApi;
    private SyncApi syncApi;
    private BotProfileApi botProfileApil;
    private StatusApi statusApi;

    private DialogExecutor executor;
    private AsyncHttpClient asyncHttpClient;

    private volatile CompletableFuture<Bot> voidCompletableFuture;

    private Bot() {
        this.executor = new DialogExecutor(4);
    }

    private Bot(BotConfig botConfig) {
        this.botConfig = botConfig;
        if (botConfig.getParallelism() > 0) {
            this.executor = new DialogExecutor(botConfig.getParallelism());
        }
        else {
            this.executor = new DialogExecutor(4);
        }

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
        instance.asyncHttpClient = createHttpClient(botConfig);
        instance.internalBotApi = new InternalBotApi(botConfig, instance.executor, instance.asyncHttpClient);

        instance.voidCompletableFuture = instance.internalBotApi.start().thenAccept(v -> {
            instance.runApis(instance.internalBotApi);
        }).thenCompose(aVoid -> CompletableFuture.completedFuture(instance));

        return instance.voidCompletableFuture;
    }

    /**
     * Launches the bot with full config
     *
     * @param botConfig the constructed config object
     * @return Future that completes when bot authorised
     */
    public static CompletableFuture<Bot> start(BotConfig botConfig) {
        Bot instance = new Bot();
        instance.asyncHttpClient = createHttpClient(botConfig);
        instance.internalBotApi = new InternalBotApi(botConfig, instance.executor, instance.asyncHttpClient);

        instance.voidCompletableFuture = instance.internalBotApi.start().thenAccept(v -> {
            instance.runApis(instance.internalBotApi);
        }).thenCompose(aVoid -> CompletableFuture.completedFuture(instance));

        System.setProperty("java.awt.headless", "true");

        return instance.voidCompletableFuture;
    }

    private void runApis(InternalBotApi internalBotApi) {
        messagingApi = new MessagingApi(internalBotApi);
        users = new UsersApi(internalBotApi);
        interactiveApi = new InteractiveApi(internalBotApi);
        mediaAndFilesApi = new MediaAndFilesApi(internalBotApi);
        groupsApi = new GroupsApi(internalBotApi);
        peersApi = new PeersApi(internalBotApi);
        syncApi = new SyncApi(internalBotApi);
        botProfileApil = new BotProfileApi(internalBotApi);
        statusApi = new StatusApi(internalBotApi);
    }

    private static AsyncHttpClient createHttpClient(BotConfig botConfig) {
        final DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
        if (botConfig.getCertPath() != null && botConfig.getCertPassword() != null) {
            try {
                SslContext sslContext = SslContextBuilder.forClient()
                        .keyManager(NetUtils.createKeyFactory(new File(botConfig.getCertPath()),
                                botConfig.getCertPassword())).build();
                builder.setSslContext(sslContext);

            } catch (Exception e) {
                log.error("Failed to create http client", e);
            }
        }
        return asyncHttpClient(builder);
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
            if (!asyncHttpClient.isClosed()) {
                try {
                    asyncHttpClient.close();
                } catch (IOException e) {
                    log.error("Failed to stop bot", e);
                }
            }
            stopLock.notifyAll();
        }
    }

    private void lock() {
        try {
            voidCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to take a lock", e);
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

    public GroupsApi groupsApi() {
        lock();
        return groupsApi;
    }

    public PeersApi peersApi() {
        lock();
        return peersApi;
    }

    public SyncApi syncApi() {
        lock();
        return syncApi;
    }

    public BotProfileApi botProfileApi() {
        lock();
        return botProfileApil;
    }

    public StatusApi statusApi() {
        lock();
        return statusApi;
    }

}
