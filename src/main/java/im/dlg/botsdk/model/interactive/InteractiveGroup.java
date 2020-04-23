package im.dlg.botsdk.model.interactive;

import java.util.ArrayList;
import java.util.List;

public class InteractiveGroup {
    private String title;
    private String description;
    private List<InteractiveAction> actions = new ArrayList<>();

    public InteractiveGroup(String title, String description, List<InteractiveAction> actions) {
        this.title = title;
        this.description = description;
        this.actions = actions;
    }

    public InteractiveGroup(List<InteractiveAction> actions) {
        this(null, null, actions);
    }


    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<InteractiveAction> getActions() {
        return actions;
    }
}
