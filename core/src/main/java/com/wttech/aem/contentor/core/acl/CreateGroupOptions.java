package com.wttech.aem.contentor.core.acl;

public class CreateGroupOptions {

    private String id;

    private String externalId;

    private Mode mode;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void skipIfExists() {
        mode = Mode.SKIP;
    }

    public void overrideIfExists() {
        mode = Mode.OVERRIDE;
    }

    public void failIfExists() {
        mode = Mode.FAIL;
    }

    public enum Mode {
        OVERRIDE, SKIP, FAIL
    }
}
