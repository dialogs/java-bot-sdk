package im.dlg.botsdk.model.content;

import dialog.MediaAndFilesOuterClass;
import dialog.MessagingOuterClass;

public class DocumentContent extends Content {

    private long fileId;
    private long accessHash;
    private int fileSize;
    private String name;
    private String mimeType;
    private MediaAndFilesOuterClass.FastThumb fastThumb;
    private MessagingOuterClass.DocumentEx ext;

    DocumentContent(MessagingOuterClass.DocumentMessage document) {
        this.fileId = document.getFileId();
        this.accessHash = document.getAccessHash();
        this.fileSize = document.getFileSize();
        this.name = document.getName();
        this.mimeType = document.getMimeType();
        this.fastThumb = document.getThumb();
        this.ext = document.getExt();
    }

    public long getFileId() {
        return fileId;
    }

    public long getAccessHash() {
        return accessHash;
    }

    public int getFileSize() {
        return fileSize;
    }

    public String getName() {
        return name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public MediaAndFilesOuterClass.FastThumb getFastThumb() {
        return fastThumb;
    }

    public MessagingOuterClass.DocumentEx getExt() {
        return ext;
    }

    public static MessagingOuterClass.DocumentMessage createDocumentMessage(DocumentContent content) {
        MediaAndFilesOuterClass.FastThumb thumb = content.getFastThumb();
        MediaAndFilesOuterClass.FastThumb fastThumb = MediaAndFilesOuterClass.FastThumb
                .newBuilder()
                .setH(thumb.getH())
                .setW(thumb.getW())
                .setThumb(thumb.getThumb())
                .build();

        return MessagingOuterClass.DocumentMessage
                .newBuilder()
                .setFileId(content.getFileId())
                .setAccessHash(content.getAccessHash())
                .setFileSize(content.getFileSize())
                .setName(content.getName())
                .setMimeType(content.getMimeType())
                .setThumb(fastThumb)
                .setExt(content.getExt())
                .build();
    }
}
