package im.dlg.botsdk.reminderbot;

import im.dlg.botsdk.reminderbot.ReminderManager;
import im.dlg.botsdk.Bot;
import java.util.concurrent.ExecutionException;

public class ReminderBot {
    private static final String API_KEY = "e7950f126f6957c44abb61fd268577c9e0697e68";

    private static ReminderBot instance;
    private final Bot bot;
    private final ReminderManager reminderManager;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        new ReminderBot(API_KEY);
    }

    private ReminderBot(String key) throws InterruptedException, ExecutionException {
        instance = this;
        this.bot = Bot.start(key).get();
        this.reminderManager = new ReminderManager();

        debug("Bot started!");

        ReminderBotListener listener = new ReminderBotListener();
        bot.messaging().onMessage(listener);
        bot.interactiveApi().onEvent(listener);
        bot.await();
    }

    public static ReminderBot getInstance() {
        return instance;
    }

    public Bot getBot() {
        return bot;
    }

    public ReminderManager getReminderManager() {
        return reminderManager;
    }

    public void debug(String msg) {
        System.out.println("DEBUG: " + msg);
    }
}
