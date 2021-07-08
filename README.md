Java Bot SDK
============

Dialog Java Bot SDK.

[Documentation](https://docs.dlg.im/bots/java-bot.html)

[API Reference](https://botapi.dlg.im/overview-summary.html)

[Example](https://github.com/dialogs/java-bot-sdk/tree/master/examples)

[Community chat](https://dlg.im/@botsfordialog)


How to use the SDK with maven/gradle?

1) Add repository 

Gradle:
```
repositories {
    maven { url "http://dialog.bintray.com/maven" }
}
```

2) Add BOT dependency
```
dependencies {
    compile 'im.dlg:bot-sdk:2.0.1'
    compile 'org.slf4j:slf4j-log4j12:1.7.27' //or any other slf4j provider
}
```

3) Create bot and subscribe on updates
``` java
public class Main {
    public static void main(String[] args) throws InterruptedException, ExecutionException {

        BotConfig botConfig = BotConfig.Builder.aBotConfig()
                .withHost("your-server-host")
                .withPort(443)
                .withToken("your-bot-token")
                .build();

        Bot bot = Bot.start(botConfig).get();

        bot.messaging().onMessage(message ->
                bot.users().get(message.getSender()).thenAccept(userOpt -> userOpt.ifPresent(user -> {
                            System.out.println("Got a message: " + message.getText() + " from user: " + user.getName());
                        })
                ).thenCompose(aVoid -> {
                            return bot.messaging().sendText(message.getPeer(), "pong");
                        }
                ).exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                }).thenAccept(uuid ->
                        System.out.println("Sent a message with UUID: " + uuid)));

        bot.await();
    }
}
```

