package com.wttech.aem.contentor.core.acl;

import java.util.HashMap;
import java.util.Map;

public class CreateAuthorizableOptions {

    private String id;

    private String path;

    protected String givenName;

    private String email;

    private String aboutMe;

    private final Map<String, String> properties = new HashMap<>();

    private CreateGroupOptions.Mode mode = CreateGroupOptions.Mode.OVERRIDE;

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

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties.putAll(properties);
    }

    public void property(String key, String value) {
        properties.put(key, value);
    }

    public Map<String, String> determineAllProperties() {
        Map<String, String> allProperties = new HashMap<>(properties);
        allProperties.compute("profile/givenName", (key, value) -> givenName);
        allProperties.compute("profile/email", (key, value) -> email);
        allProperties.compute("profile/aboutMe", (key, value) -> aboutMe);
        return allProperties;
    }

    public CreateGroupOptions.Mode getMode() {
        return mode;
    }

    public void setMode(CreateGroupOptions.Mode mode) {
        this.mode = mode;
    }

    public void skipIfExists() {
        mode = CreateGroupOptions.Mode.SKIP;
    }

    public void overrideIfExists() {
        mode = CreateGroupOptions.Mode.OVERRIDE;
    }

    public void failIfExists() {
        mode = CreateGroupOptions.Mode.FAIL;
    }

    public enum Mode {
        OVERRIDE,
        SKIP,
        FAIL
    }
}
