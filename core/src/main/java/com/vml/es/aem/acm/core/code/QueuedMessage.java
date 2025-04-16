package com.vml.es.aem.acm.core.code;

import com.vml.es.aem.acm.core.AcmException;
import com.vml.es.aem.acm.core.util.JsonUtils;
import java.io.IOException;
import java.io.Serializable;

public class QueuedMessage implements Serializable {

    private ExecutionStatus status;

    public QueuedMessage() {
        // for deserialization
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public static QueuedMessage of(ExecutionStatus status) {
        QueuedMessage message = new QueuedMessage();
        message.status = status;
        return message;
    }

    public String toJson() {
        try {
            return JsonUtils.writeToString(this);
        } catch (IOException e) {
            throw new AcmException("Cannot serialize queued message!", e);
        }
    }

    public static QueuedMessage fromJson(String json) {
        try {
            return JsonUtils.readFromString(json, QueuedMessage.class);
        } catch (IOException e) {
            throw new AcmException("Cannot deserialize queued message!", e);
        }
    }
}
