package im.dlg.botsdk.model.content;

import java.util.List;
import java.util.stream.IntStream;

import dialog.MessagingOuterClass;
import dialog.Miscellaneous;

public class TextContent extends Content {

    private String text;
    private MessagingOuterClass.TextMessageEx textMessageEx;
    private List<Integer> mentions;
    private List<MessagingOuterClass.MessageMedia> medias;
    private List<Miscellaneous.Any> extensions;

    TextContent(MessagingOuterClass.TextMessage content) {
        this.text = content.getText();
        this.textMessageEx = content.getExt();
        this.mentions = content.getMentionsList();
        this.medias = content.getMediaList();
        this.extensions = content.getExtensionsList();
    }

    public String getText() {
        return text;
    }

    public MessagingOuterClass.TextMessageEx getTextMessageEx() {
        return textMessageEx;
    }

    public List<Integer> getMentions() {
        return mentions;
    }

    public List<MessagingOuterClass.MessageMedia> getMedias() {
        return medias;
    }

    public List<Miscellaneous.Any> getExtensions() {
        return extensions;
    }

    public static MessagingOuterClass.TextMessage createTextMessage(TextContent content) {
        MessagingOuterClass.TextMessage.Builder textMessage = MessagingOuterClass.TextMessage
                .newBuilder();
        List<MessagingOuterClass.MessageMedia> medias = content.medias;
        if (medias != null) {
            IntStream.range(0, medias.size())
                    .forEach(index ->
                            textMessage.setMedia(index, medias.get(index)));
        }

        List<Miscellaneous.Any> extensions = content.extensions;
        if (extensions != null) {
            IntStream.range(0, extensions.size())
                    .forEach(index ->
                            textMessage.setExtensions(index, extensions.get(index)));
        }
        textMessage.setExt(content.textMessageEx);
        return textMessage.build();
    }
}
