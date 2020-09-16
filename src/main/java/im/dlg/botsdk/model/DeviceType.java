package im.dlg.botsdk.model;

import im.dlg.grpc.services.PresenceOuterClass;

import static im.dlg.grpc.services.PresenceOuterClass.DeviceType.*;

public enum DeviceType {
    UNKNOWN,
    GENERIC,
    PC,
    MOBILE,
    TABLET,
    WATCH,
    MIRROR,
    CAR,
    TABLE;

    public PresenceOuterClass.DeviceType toGrpcType() {
        switch (this) {
            case GENERIC:
                return DEVICETYPE_GENERIC;
            case PC:
                return DEVICETYPE_PC;
            case MOBILE:
                return DEVICETYPE_MOBILE;
            case TABLET:
                return DEVICETYPE_TABLET;
            case WATCH:
                return DEVICETYPE_WATCH;
            case MIRROR:
                return DEVICETYPE_MIRROR;
            case CAR:
                return DEVICETYPE_CAR;
            case TABLE:
                return DEVICETYPE_TABLE;
        }

        return DEVICETYPE_UNKNOWN;
    }

    public static DeviceType fromRawInt(int value) {
        if (value < 0 || value > DeviceType.values().length) {
            return DeviceType.UNKNOWN;
        }

        return DeviceType.values()[value];
    }

}