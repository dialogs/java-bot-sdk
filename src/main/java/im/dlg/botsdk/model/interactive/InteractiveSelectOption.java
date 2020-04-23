package im.dlg.botsdk.model.interactive;

/**
 * Option item for selection menu
 */
public class InteractiveSelectOption {
    private String value;
    private String label;

    public InteractiveSelectOption(String value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * @return option label
     */
    public String getValue() {
        return value;
    }

    /**
     * @return text label
     */
    public String getLabel() {
        return label;
    }
}
