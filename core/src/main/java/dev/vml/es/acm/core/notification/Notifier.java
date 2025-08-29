package dev.vml.es.acm.core.notification;

import java.io.Closeable;

public interface Notifier<M> extends Closeable {

    String getId();

    boolean isEnabled();

    void sendMessage(M message);
}
