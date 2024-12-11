package com.wttech.aem.contentor.core.acl;

public class CreateUserOptions {

    private String id;

    private String password;

    private String givenName;

    private String familyName;

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

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public void system() {
        this.system = true;
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
