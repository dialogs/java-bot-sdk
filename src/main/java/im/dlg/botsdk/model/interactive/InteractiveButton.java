package im.dlg.botsdk.model.interactive;

/**
 * Button element
 */
public class InteractiveButton implements InteractiveWidget {
    private String value;
    private String label;

    public InteractiveButton(String value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * @return the value, text indication for element
     */
    public String getValue() {
        return value;
    }

    /**
     * @return the text label on button
     */
    public String getLabel() {
        return label;
    }
}
