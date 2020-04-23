package im.dlg.botsdk.model;

import dialog.UsersOuterClass;

public class User {

    public enum Sex {
        MALE,
        FEMALE;

        public static Sex fromServerModel(UsersOuterClass.Sex sex) {
            return sex.getNumber() == 3 ? MALE : FEMALE;
        }
    }

    private Peer peer;
    private String name;
    private String nick;
    private String about;
    private Sex sex;
    private String language;
    private String timeZone;
    private String customProfile;

    public User(Peer peer, String name, String nick, Sex sex, String about, String language, String timeZone) {
        this(peer, name, nick, sex, about, language, timeZone, null);
    }

    public User(Peer peer, String name, String nick, Sex sex, String about, String language, String timeZone, String customProfile) {
        this.peer = peer;
        this.name = name;
        this.nick = nick;
        this.about = about;
        this.sex = sex;
        this.language = language;
        this.timeZone = timeZone;
        this.customProfile = customProfile;
    }

    /**
     * @return The peer, related to user
     */
    public Peer getPeer() {
        return peer;
    }

    /**
     * @return The name of user
     */
    public String getName() {
        return name;
    }

    /**
     * @return Nickname
     */
    public String getNick() {
        return nick;
    }

    /**
     * @return About string info
     */
    public String getAbout() {
        return about;
    }

    /**
     * @return user sex
     */
    public Sex getSex() {
        return sex;
    }

    /**
     * @return user lang
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @return user timezone
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * @return Json string of custom profile
     */
    public String getCustomProfile() {
        return customProfile;
    }

    @Override
    public String toString() {
        return "User{" +
                "peer=" + peer +
                ", name='" + name + '\'' +
                ", nick='" + nick + '\'' +
                ", about='" + about + '\'' +
                ", sex=" + sex +
                ", language='" + language + '\'' +
                ", timeZone='" + timeZone + '\'' +
                '}';
    }
}
