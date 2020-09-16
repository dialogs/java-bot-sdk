package im.dlg.botsdk.model.media;

import com.google.protobuf.StringValue;

import im.dlg.grpc.services.MessagingOuterClass;

public class WebPageMedia {
    private String url;
    private String title;
    private String description;
    private ImageLocation image;

    public WebPageMedia(String url, String title, String description, ImageLocation image) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.image = image;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ImageLocation getImage() {
        return image;
    }

    public static MessagingOuterClass.WebpageMedia buildWebpage(WebPageMedia webPageMedia) {
        return MessagingOuterClass.WebpageMedia
                .newBuilder()
                .setUrl(StringValue.of(webPageMedia.getUrl()))
                .setTitle(StringValue.of(webPageMedia.getTitle()))
                .setDescription(StringValue.of(webPageMedia.getDescription()))
                .build();
    }
}
