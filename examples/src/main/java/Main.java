import im.dlg.botsdk.Bot;
import im.dlg.botsdk.BotConfig;
import im.dlg.botsdk.BotSystem;
import im.dlg.botsdk.BotSystemConfig;
import im.dlg.botsdk.model.content.DocumentContent;
import im.dlg.botsdk.model.content.TextContent;

public class Main {

    public static void main(String[] args) throws Exception {

        BotSystemConfig systemConfig = BotSystemConfig.Builder.newBuilder()
                .withHost("")
                .withPort(443)
                .build();

        BotSystem system = BotSystem.createSystem(systemConfig);

        BotConfig botConfig = BotConfig.Builder.newBuilder().build();
        Bot bot = system.startBot(botConfig).get();

        bot.messaging().onMessage(message ->
                bot.users().get(message.getSender()).thenAccept(userOpt -> userOpt.ifPresent(user -> {
                            System.out.println("Got a message: " + message.getText() + " from user: " + user.getName());
                        })
                ).thenCompose(aVoid -> {
                            switch (message.getText()) {
                                case "Send me photo":
                                    return bot.messaging().sendMedia(message.getPeer(), ((TextContent) message.getMessageContent()).getMedias());
                                case "Send me video":
                                    return bot.messaging().sendDocument(message.getPeer(), (DocumentContent) message.getMessageContent());
                                default:
                                    return bot.messaging().reply(message.getPeer(), message.getMessageId(), "some reply");
                            }
                        }
                ).exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                }).thenAccept(uuid ->
                        System.out.println("Sent a message with UUID: " + uuid)));

        bot.await();
    }
}