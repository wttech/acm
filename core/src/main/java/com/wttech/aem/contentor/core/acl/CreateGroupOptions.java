package com.wttech.aem.contentor.core.acl;

public class CreateGroupOptions extends CreateAuthorizableOptions {

    private String externalId;

    public static CreateGroupOptions of(
            String id, String externalId, String path, String givenName, String email, String aboutMe, Mode mode) {
        CreateGroupOptions options = new CreateGroupOptions();
        options.setId(id);
        options.externalId = externalId;
        options.setPath(path);
        options.setGivenName(givenName);
        options.setEmail(email);
        options.setAboutMe(aboutMe);
        options.setMode(mode);
        return options;
    }

    public static CreateGroupOptions of(String id, String externalId) {
        CreateGroupOptions options = new CreateGroupOptions();
        options.setId(id);
        options.externalId = externalId;
        return options;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
}
