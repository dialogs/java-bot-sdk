package im.dlg.botsdk.model.media;

import im.dlg.grpc.services.MediaAndFilesOuterClass;

public class AudioLocation {
    private FileLocation fileLocation;
    private int duration;
    private String mimeType;
    private int fileSize;

    public AudioLocation(FileLocation fileLocation, int duration, String mimeType, int fileSize) {
        this.fileLocation = fileLocation;
        this.duration = duration;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
    }

    public FileLocation getFileLocation() {
        return fileLocation;
    }

    public int getDuration() {
        return duration;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getFileSize() {
        return fileSize;
    }

    public static MediaAndFilesOuterClass.AudioLocation buildAudioLocation(AudioLocation audioLocation) {
        return MediaAndFilesOuterClass.AudioLocation
                .newBuilder()
                .setDuration(audioLocation.getDuration())
                .setMimeType(audioLocation.getMimeType())
                .setFileSize(audioLocation.getFileSize())
                .setFileLocation(FileLocation.buildFileLocation(
                        new FileLocation(audioLocation.getFileLocation().getFileId(),
                                audioLocation.getFileLocation().getAccessHash())))
                .build();
    }
}
