package im.dlg.botsdk.model.interactive;

public class InteractiveAction {

    public enum Style {
        PRIMARY,
        DANGER,
        DEFAULT
    }

    private String id;
    private Style style;
    private InteractiveWidget widget;
    private InteractiveConfirm confirm;

    public InteractiveAction(String id, Style style, InteractiveWidget widget, InteractiveConfirm confirm) {
        this.id = id;
        this.style = style;
        this.widget = widget;
        this.confirm = confirm;
    }

    public InteractiveAction(String id, InteractiveWidget widget, InteractiveConfirm confirm) {
        this(id, Style.PRIMARY, widget, confirm);
    }

    public InteractiveAction(String id, InteractiveWidget widget) {
        this(id, Style.PRIMARY, widget, null);
    }

    public String getId() {
        return id;
    }

    public Style getStyle() {
        return style;
    }

    public InteractiveWidget getWidget() {
        return widget;
    }

    public InteractiveConfirm getConfirm() {
        return confirm;
    }
}
