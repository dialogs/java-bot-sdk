package im.dlg.botsdk.reminderbot;

import im.dlg.botsdk.reminderbot.Reminder;

import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class ReminderManager {
    private static final String REMINDER_MESSAGE = "Hey! You asked to remind: ";

    private final ReminderBot instance;
    private final PriorityQueue<Reminder> reminderQueue;
    private final TreeMap<Integer, ReminderRequest> reminderRequests;
    private int requestCount = 0;

    public ReminderManager() {
        this.instance = ReminderBot.getInstance();
        this.reminderQueue = new PriorityQueue<>();
        this.reminderRequests = new TreeMap<>();

        // Launch check for reminders every second
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                check();
            }
        }, 1000L, 1000L);
    }

    private void check() {
        while (reminderQueue.size() > 0) {
            Reminder reminder = reminderQueue.peek();

            if (reminder.getUnixTime() > System.currentTimeMillis()) {
                return;
            }

            reminderQueue.remove(reminder);

            instance.getBot().messaging()
                .send(reminder.getPeer(), REMINDER_MESSAGE+reminder.getMessage())
                .thenAccept(uuid -> instance.debug("Sent reminder."));
        }
    }

    public void addReminder(Reminder reminder) {
        instance.debug("Added reminder: "+reminder.getMessage()+" at "+reminder.getUnixTime());
        reminderQueue.offer(reminder);
    }

    public int getNextRequestId() {
        return requestCount++;
    }

    public ReminderRequest getReminderRequest(int id) {
        return reminderRequests.get(id);
    }

    public void updateReminderRequest(int id, ReminderRequest req) {
        reminderRequests.put(id, req);
    }

    public void deleteReminderRequest(int id) {
        reminderRequests.remove(id);
    }
}
