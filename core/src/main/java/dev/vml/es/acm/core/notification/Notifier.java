package dev.vml.es.acm.core.notification;

public interface Notifier<M> {

    String getId();

    void sendMessage(M message);

    boolean isEnabled();
}
