package im.dlg.botsdk.model.interactive;

import java.util.ArrayList;
import java.util.List;

/**
 * Selection dropdown
 */
public class InteractiveSelect implements InteractiveWidget {

    private String label;
    private String defaultValue;
    private List<InteractiveSelectOption> options = new ArrayList<>();

    public InteractiveSelect(String label, String defaultValue, List<InteractiveSelectOption> options) {
        this.label = label;
        this.defaultValue = defaultValue;
        this.options = options;
    }

    public InteractiveSelect(List<InteractiveSelectOption> options) {
        this.options = options;
    }

    public String getLabel() {
        return label;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public List<InteractiveSelectOption> getOptions() {
        return options;
    }
}
