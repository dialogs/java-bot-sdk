package im.dlg.botsdk.model.content;

import java.util.List;
import java.util.stream.IntStream;

import im.dlg.grpc.services.MessagingOuterClass;
import im.dlg.grpc.services.Miscellaneous;

import static im.dlg.grpc.services.MessagingOuterClass.*;

public class TextContent extends Content {

    private final String text;
    private final TextMessageEx textMessageEx;
    private final List<Mention> mentions;
    private final List<MessageMedia> medias;
    private final List<Miscellaneous.Any> extensions;

    TextContent(TextMessage content) {
        this.text = content.getText();
        this.textMessageEx = content.getExt();
        this.mentions = content.getMentionsList();
        this.medias = content.getMediaList();
        this.extensions = content.getExtensionsList();
    }

    public String getText() {
        return text;
    }

    public TextMessageEx getTextMessageEx() {
        return textMessageEx;
    }

    public List<Mention> getMentions() {
        return mentions;
    }

    public List<MessageMedia> getMedias() {
        return medias;
    }

    public List<Miscellaneous.Any> getExtensions() {
        return extensions;
    }

    public static TextMessage createTextMessage(TextContent content) {
        TextMessage.Builder textMessage = TextMessage
                .newBuilder();
        List<MessageMedia> medias = content.medias;
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
