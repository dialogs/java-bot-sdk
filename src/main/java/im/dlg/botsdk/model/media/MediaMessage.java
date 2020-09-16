package im.dlg.botsdk.model.media;

import java.util.List;

import im.dlg.grpc.services.MessagingOuterClass;
import im.dlg.botsdk.model.interactive.InteractiveGroup;

public class MediaMessage {
    private ImageMedia imageMedia;
    private AudioMedia audioMedia;
    private WebPageMedia webPageMedia;
    private List<InteractiveGroup> actions;

    public MediaMessage(ImageMedia imageMedia,
                        AudioMedia audioMedia,
                        WebPageMedia webPageMedia,
                        List<InteractiveGroup> actions) {
        this.imageMedia = imageMedia;
        this.audioMedia = audioMedia;
        this.webPageMedia = webPageMedia;
        this.actions = actions;
    }

    public ImageMedia getImageMedia() {
        return imageMedia;
    }

    public AudioMedia getAudioMedia() {
        return audioMedia;
    }

    public WebPageMedia getWebPageMedia() {
        return webPageMedia;
    }

    public List<InteractiveGroup> getActions() {
        return actions;
    }

    public static MessagingOuterClass.MessageMedia buildMedia(MediaMessage media) {
        MessagingOuterClass.MessageMedia.Builder msg = MessagingOuterClass.MessageMedia.newBuilder();
        if (media.getImageMedia() != null) {
            MessagingOuterClass.ImageMedia image = MessagingOuterClass.ImageMedia
                    .newBuilder()
                    .setImage(ImageLocation.buildImageLocation(media.getImageMedia().getImageLocation()))
                    .build();
            msg.setImage(image);
        }

        if (media.getAudioMedia() != null) {
            MessagingOuterClass.AudioMedia audio = MessagingOuterClass.AudioMedia
                    .newBuilder()
                    .setAudio(AudioLocation.buildAudioLocation(media.getAudioMedia().getAudioLocation()))
                    .build();
            msg.setAudio(audio);
        }

        if (media.getWebPageMedia() != null) {
            MessagingOuterClass.WebpageMedia webpage = WebPageMedia.buildWebpage(media.getWebPageMedia());
            msg.setWebpage(webpage);
        }

        return msg.build();
    }
}
