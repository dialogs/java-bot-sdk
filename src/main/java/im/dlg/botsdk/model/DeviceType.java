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
            default:
                return DEVICETYPE_UNKNOWN;
        }
    }

    public static DeviceType fromGrpcType(PresenceOuterClass.DeviceType deviceType) {
        switch (deviceType) {
            case DEVICETYPE_GENERIC:
                return DeviceType.GENERIC;
            case DEVICETYPE_PC:
                return DeviceType.PC;
            case DEVICETYPE_MOBILE:
                return DeviceType.MOBILE;
            case DEVICETYPE_TABLET:
                return DeviceType.TABLET;
            case DEVICETYPE_WATCH:
                return DeviceType.WATCH;
            case DEVICETYPE_MIRROR:
                return DeviceType.MIRROR;
            case DEVICETYPE_CAR:
                return DeviceType.CAR;
            case DEVICETYPE_TABLE:
                return DeviceType.TABLE;
            default:
                return DeviceType.UNKNOWN;
        }
    }

}