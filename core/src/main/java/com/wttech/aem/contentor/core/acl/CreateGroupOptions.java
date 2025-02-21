package com.wttech.aem.contentor.core.acl;

public class CreateGroupOptions extends CreateAuthorizableOptions {

    private String externalId;

    public CreateGroupOptions() {}

    public CreateGroupOptions(
            String id, String externalId, String path, String givenName, String email, String aboutMe, Mode mode) {
        super(id, path, givenName, email, aboutMe, mode);
        this.externalId = externalId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
}
