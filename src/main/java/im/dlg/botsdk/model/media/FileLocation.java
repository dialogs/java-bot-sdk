package im.dlg.botsdk.model.media;

import dialog.MediaAndFilesOuterClass;

public class FileLocation {
    private long fileId;
    private long accessHash;

    public FileLocation(long fileId, long accessHash) {
        this.fileId = fileId;
        this.accessHash = accessHash;
    }

    public long getFileId() {
        return fileId;
    }

    public long getAccessHash() {
        return accessHash;
    }

    public static MediaAndFilesOuterClass.FileLocation buildFileLocation(FileLocation fileLocation) {
        return MediaAndFilesOuterClass.FileLocation
                .newBuilder()
                .setFileId(fileLocation.getFileId())
                .setAccessHash(fileLocation.getAccessHash())
                .build();
    }
}
