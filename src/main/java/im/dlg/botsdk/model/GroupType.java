package im.dlg.botsdk.model;

import im.dlg.grpc.services.GroupsOuterClass;

public enum GroupType {
    GROUPTYPE_GROUP,
    GROUPTYPE_CHANNEL;

    public static GroupType fromServer(GroupsOuterClass.GroupType groupType) {
        return groupType == GroupsOuterClass.GroupType.GROUPTYPE_GROUP ? GROUPTYPE_GROUP : GROUPTYPE_CHANNEL;
    }
}