package im.dlg.botsdk.model;

public class Group {
    private String shortname;
    private String title;
    private Peer peer;
    private GroupType type;

    public Group(String shortname, String title, Peer peer, GroupType type) {
        this.shortname = shortname;
        this.title = title;
        this.peer = peer;
        this.type = type;
    }

    /**
     * @return The peer, related to group
     */
    public Peer getPeer() {
        return peer;
    }

    /**
     * @return The title of group
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return Shortname
     */
    public String getShortname() {
        return shortname;
    }

    /**
     * @return About string info
     */
    public GroupType getType() {
        return type;
    }

    /**
     * @return user sex
     */

    @Override
    public String toString() {
        return "Group{" +
                "peer=" + peer +
                ", title='" + title + '\'' +
                ", shortname='" + shortname + '\'' +
                ", type='" + type +
                '}';
    }
}
