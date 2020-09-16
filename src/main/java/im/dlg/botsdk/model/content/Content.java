package im.dlg.botsdk.model.content;

import im.dlg.grpc.services.MessagingOuterClass;

public abstract class Content {
    public static Content fromMessage(MessagingOuterClass.MessageContent message) {
        int key = message.getBodyCase().getNumber();
        switch (key) {
            case 1:
                return new TextContent(message.getTextMessage());
            case 2:
                return new ServiceContent(message.getServiceMessage());
            case 3:
                return new DocumentContent(message.getDocumentMessage());
            case 4:
                return new JsonContent(message.getJsonMessage());
            case 5:
                return new UnsupportedContent();
            case 6:
                return new UnsupportedContent();
            case 7:
                return new UnsupportedContent();
            case 8:
                return new EmptyContent(message.getEmptyMessage());
            case 9:
                return new UnsupportedContent();
        }
        return new UnsupportedContent();
    }
}
