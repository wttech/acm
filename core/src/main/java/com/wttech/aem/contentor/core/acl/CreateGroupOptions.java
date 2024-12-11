package com.wttech.aem.contentor.core.acl;

import java.util.HashMap;
import java.util.Map;

public class CreateGroupOptions {

    private String id;

    private String path;

    private String givenName;

    private String email;

    private String aboutMe;

    private String externalId;

    private Mode mode;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAboutMe() {
        return aboutMe;
    }

    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
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

    public Map<String, String> determineProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.compute("profile/givenName", (key, value) -> givenName);
        properties.compute("profile/email", (key, value) -> email);
        properties.compute("profile/aboutMe", (key, value) -> aboutMe);
        return properties;
    }

    public enum Mode {
        OVERRIDE, SKIP, FAIL
    }
}
