package dev.vml.es.acm.core.acl;

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
            setGivenName(parts[0]);
            familyName = parts[1];
        } else {
            throw new IllegalArgumentException(String.format(
                    "Full name '%s' must contain exactly two parts space-delimited: given name and family name",
                    fullName));
        }
    }

    @Override
    public Map<String, String> determineProperties() {
        Map<String, String> properties = new HashMap<>(super.determineProperties());
        properties.compute("profile/familyName", (key, value) -> familyName);
        return properties;
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
