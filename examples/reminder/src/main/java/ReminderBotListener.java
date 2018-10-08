package im.dlg.botsdk.reminderbot;

import im.dlg.botsdk.reminderbot.Reminder;
import im.dlg.botsdk.reminderbot.ReminderManager;

import im.dlg.botsdk.domain.Message;
import im.dlg.botsdk.light.MessageListener;
import im.dlg.botsdk.light.InteractiveEventListener;
import im.dlg.botsdk.domain.InteractiveEvent;
import im.dlg.botsdk.domain.interactive.InteractiveAction;
import im.dlg.botsdk.domain.interactive.InteractiveButton;
import im.dlg.botsdk.domain.interactive.InteractiveGroup;
import im.dlg.botsdk.domain.interactive.InteractiveSelect;
import im.dlg.botsdk.domain.interactive.InteractiveSelectOption;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

public class ReminderBotListener implements MessageListener, InteractiveEventListener {
    private static final String FIRST_MESSAGE = "Hi! You can send me a message enclosed in quotes and I will remind you about it at the right time.";
    private static final String SETUP_TIME_MESSAGE = "Ok! When do you need me to remind you of this?";
    private static final String OK_MESSAGE = "Ok, got it! I will remind you at the right time.";
    private static final String[] SETUP_TIME_BUTTON_LABELS = {"In 5 seconds", "In 30 minutes", "In 1 hour", "In 2 hours", "Tomorrow", "A week later", "Indicate the time"};
    private static final String[] SETUP_TIME_BUTTON_VALUES = {"5s", "30m", "1h", "2h", "1d", "7d", "more"};

    private final ReminderBot instance;

    public ReminderBotListener() {
        this.instance = ReminderBot.getInstance();
    }

    @Override
    public void onMessage(Message message) {
        String text = message.getText().trim();
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            int id = instance.getReminderManager().getNextRequestId();

            List<InteractiveAction> actionsList = new ArrayList<>();
            for (int i = 0; i < SETUP_TIME_BUTTON_LABELS.length; i++) {
                actionsList.add(new InteractiveAction(SETUP_TIME_BUTTON_VALUES[i]+"+"+id,
                                                      new InteractiveButton(SETUP_TIME_BUTTON_VALUES[i]+"+"+id,
                                                                            SETUP_TIME_BUTTON_LABELS[i])));
            }

            instance.getReminderManager()
                .updateReminderRequest(id, new ReminderRequest(message.getPeer(), text));

            instance.getBot().messaging()
                .send(message.getPeer(), SETUP_TIME_MESSAGE);

            instance.getBot().interactiveApi()
                .send(message.getPeer(), new InteractiveGroup(actionsList))
                .thenAccept(uuid -> instance.debug("Initiated reminder request ("+id+"): " + text));
        } else {
            instance.getBot().messaging()
                .send(message.getPeer(), FIRST_MESSAGE)
                .thenAccept(uuid -> instance.debug("Sent first message."));
        }
    }

    @Override
    public void onEvent(InteractiveEvent event) {
        String text = event.getValue();
        instance.debug("Received event: "+text);

        int plusPosition = text.indexOf('+');
        String cmd = text.substring(0, plusPosition);
        int id = Integer.parseInt(text.substring(plusPosition+1));

        if (cmd.equals("more")) {
            List<InteractiveSelectOption> hourOptions = new ArrayList<>();
            List<InteractiveSelectOption> minuteOptions = new ArrayList<>();

            for (int i = 0; i < 24; i++) hourOptions.add(new InteractiveSelectOption(Integer.toString(i)+"H+"+id, String.format("%02d", i)));
            for (int i = 0; i < 60; i++) minuteOptions.add(new InteractiveSelectOption(Integer.toString(i)+"M+"+id, String.format("%02d", i)));

            ArrayList<InteractiveAction> actions = new ArrayList<>();
            actions.add(new InteractiveAction("action_hour", new InteractiveSelect("Hour:", "Choose...", hourOptions)));
            actions.add(new InteractiveAction("action_minute", new InteractiveSelect("Minute:", "Choose...", minuteOptions)));
            InteractiveGroup interactiveGroup = new InteractiveGroup("Time selection", "Select hour and minute (UTC)", actions);

            instance.getBot().interactiveApi()
                .send(event.getPeer(), interactiveGroup);
        } else {
            long amount = Long.parseLong(cmd.substring(0, cmd.length()-1));
            String unit = cmd.substring(cmd.length() - 1);

            ReminderRequest reminderRequest = instance.getReminderManager().getReminderRequest(id);

            if (reminderRequest == null) {
                instance.getBot().messaging()
                    .send(event.getPeer(), "Please try again from the beginning.");
                return;
            }

            switch (unit) {
                case "H": reminderRequest.setHour((int) amount); break;
                case "M": reminderRequest.setMinute((int) amount); break;

                case "d": amount *= 24; // pass through
                case "h": amount *= 60;
                case "m": amount *= 60;
                case "s": amount *= 1000; reminderRequest.setDelta(amount);
            }

            Reminder reminder = reminderRequest.tryBuild();

            if (reminder != null) {
                instance.getReminderManager().addReminder(reminder);
                instance.getReminderManager().deleteReminderRequest(id);
                instance.getBot().messaging()
                    .send(event.getPeer(), OK_MESSAGE);
            } else {
                instance.getReminderManager().updateReminderRequest(id, reminderRequest);
            }
        }
    }
}
