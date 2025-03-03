package com.wttech.aem.contentor.core.instance;

import java.io.Serializable;

public class HealthIssue implements Serializable {

    private final String message;

    public HealthIssue(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
