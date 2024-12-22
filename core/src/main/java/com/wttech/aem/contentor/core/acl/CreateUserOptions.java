package com.wttech.aem.contentor.core.acl;

import java.util.HashMap;
import java.util.Map;

public class CreateUserOptions {

    private String id;

    private String path;

    private String password;

    private String givenName;

    private String familyName;

    private String email;

    private String aboutMe;

    private Map<String, String> properties = new HashMap<>();

    private boolean systemUser;

    private Mode mode = Mode.OVERRIDE;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public void setFullName(String fullName) {
        String[] parts = fullName.split(" ");
        if (parts.length == 2) {
            givenName = parts[0];
            familyName = parts[1];
        } else {
            throw new IllegalArgumentException("Full name must contain exactly two parts: given name and family name");
        }
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
        allProperties.compute("profile/familyName", (key, value) -> familyName);
        allProperties.compute("profile/email", (key, value) -> email);
        allProperties.compute("profile/aboutMe", (key, value) -> aboutMe);
        return allProperties;
    }

    public boolean isSystemUser() {
        return systemUser;
    }

    public void setSystemUser(boolean systemUser) {
        this.systemUser = systemUser;
    }

    public void systemUser() {
        this.systemUser = true;
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
        OVERRIDE,
        SKIP,
        FAIL
    }
}
