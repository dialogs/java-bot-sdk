package im.dlg.botsdk.model.interactive;

public class InteractiveConfirm {

    private String text;
    private String title;
    private String ok;
    private String dismiss;

    public InteractiveConfirm(String text, String title, String ok, String dismiss) {
        this.text = text;
        this.title = title;
        this.ok = ok;
        this.dismiss = dismiss;
    }

    public String getText() {
        return text;
    }

    public String getTitle() {
        return title;
    }

    public String getOk() {
        return ok;
    }

    public String getDismiss() {
        return dismiss;
    }
}
