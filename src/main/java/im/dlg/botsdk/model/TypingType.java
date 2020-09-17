package im.dlg.botsdk.model;

import im.dlg.grpc.services.PresenceOuterClass;

public enum TypingType {
    TEXT,
    VOICE;

    public static TypingType fromGrpcType(PresenceOuterClass.TypingType typingType) {
        switch (typingType) {
            case TYPINGTYPE_TEXT:
                return TypingType.TEXT;
            case TYPINGTYPE_VOICE:
                return TypingType.VOICE;
            default:
                return null;
        }
    }

    public PresenceOuterClass.TypingType toGrpcType() {
        switch (this) {
            case TEXT:
                return PresenceOuterClass.TypingType.TYPINGTYPE_TEXT;
            case VOICE:
                return PresenceOuterClass.TypingType.TYPINGTYPE_VOICE;
            default:
                return PresenceOuterClass.TypingType.TYPINGTYPE_UNKNOWN;
        }
    }
}
