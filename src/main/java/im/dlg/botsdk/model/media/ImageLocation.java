package im.dlg.botsdk.model.media;

import im.dlg.grpc.services.MediaAndFilesOuterClass;

public class ImageLocation {
    private FileLocation fileLocation;
    private int width;
    private int height;
    private int fileSize;

    public ImageLocation(FileLocation fileLocation, int width, int height, int fileSize) {
        this.fileLocation = fileLocation;
        this.width = width;
        this.height = height;
        this.fileSize = fileSize;
    }

    public FileLocation getFileLocation() {
        return fileLocation;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFileSize() {
        return fileSize;
    }

    public static MediaAndFilesOuterClass.ImageLocation buildImageLocation(ImageLocation imageLocation) {
        return MediaAndFilesOuterClass.ImageLocation
                .newBuilder()
                .setWidth(imageLocation.getWidth())
                .setHeight(imageLocation.getHeight())
                .setFileSize(imageLocation.getFileSize())
                .setFileLocation(FileLocation.buildFileLocation(
                        new FileLocation(imageLocation.getFileLocation().getFileId(),
                                imageLocation.getFileLocation().getAccessHash())))
                .build();
    }
}
