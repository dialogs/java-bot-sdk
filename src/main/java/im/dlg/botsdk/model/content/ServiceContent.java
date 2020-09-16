package im.dlg.botsdk.model.content;

import im.dlg.grpc.services.MessagingOuterClass;

public class ServiceContent extends Content {

    private String text;
    private MessagingOuterClass.ServiceEx serviceEx;

    public ServiceContent(MessagingOuterClass.ServiceMessage message) {
        this.text = message.getText();
        this.serviceEx = message.getExt();
    }

    public String getText() {
        return text;
    }

    public MessagingOuterClass.ServiceEx getServiceEx() {
        return serviceEx;
    }
}
