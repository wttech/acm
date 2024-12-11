package com.wttech.aem.contentor.core.acl;

import java.util.HashMap;
import java.util.Map;

public class CreateUserOptions {

    private String id;

    private String password;

    private String path;

    private String givenName;

    private String familyName;

    private String email;

    private String aboutMe;

    private Mode mode = Mode.OVERRIDE;

    private boolean system;

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

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public void system() {
        this.system = true;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
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
        properties.compute("profile/familyName", (key, value) -> familyName);
        properties.compute("profile/email", (key, value) -> email);
        properties.compute("profile/aboutMe", (key, value) -> aboutMe);
        return properties;
    }

    public enum Mode {
        OVERRIDE, SKIP, FAIL
    }
}
