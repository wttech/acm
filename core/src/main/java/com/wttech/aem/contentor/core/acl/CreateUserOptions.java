package com.wttech.aem.contentor.core.acl;

import java.util.HashMap;
import java.util.Map;

public class CreateUserOptions extends CreateAuthorizableOptions {

    private String password;

    private String familyName;

    private boolean systemUser;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Map<String, String> determineAllProperties() {
        Map<String, String> allProperties = new HashMap<>(super.determineAllProperties());
        allProperties.compute("profile/familyName", (key, value) -> familyName);
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
}
