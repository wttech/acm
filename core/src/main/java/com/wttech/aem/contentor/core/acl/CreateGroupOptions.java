package com.wttech.aem.contentor.core.acl;

public class CreateGroupOptions extends CreateAuthorizableOptions {

    private String groupId;

    private String externalId;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
}
