package im.dlg.botsdk.model.content;

import im.dlg.grpc.services.MessagingOuterClass;

public class JsonContent extends Content {
    private String rawJson;

    public JsonContent(MessagingOuterClass.JsonMessage message) {
        this.rawJson = message.getRawJson();
    }

    public String getRawJson() {
        return rawJson;
    }
}
