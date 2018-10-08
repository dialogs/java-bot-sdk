package im.dlg.botsdk.reminderbot;

import im.dlg.botsdk.reminderbot.Reminder;
import im.dlg.botsdk.domain.Peer;

import java.util.Calendar;
import java.util.TimeZone;

public class ReminderRequest{
    private Peer peer;
    private String msg;
    private int hour, minute;
    private long delta;

    public ReminderRequest(Peer peer, String msg) {
        this.peer = peer;
        this.msg = msg;
        this.hour = -1;
        this.minute = -1;
        this.delta = -1;
    }

    public void setHour(int hour) { this.hour = hour; }
    public void setMinute(int minute) { this.minute = minute; }
    public void setDelta(long delta) { this.delta = delta; }

    public Reminder tryBuild() {
        if (delta != -1) {
            return new Reminder(peer, msg, System.currentTimeMillis() + delta);
        } else if (hour != -1 && minute != -1){

            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            if (cal.getTimeInMillis() <= System.currentTimeMillis())
                cal.add(Calendar.DAY_OF_MONTH, 1);
            return new Reminder(peer, msg, cal.getTimeInMillis());
        } else {
            return null;
        }
    }
}
