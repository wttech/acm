package dev.vml.es.acm.core.acl;

public class CreateGroupOptions extends CreateAuthorizableOptions {

    private String externalId;

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
}
