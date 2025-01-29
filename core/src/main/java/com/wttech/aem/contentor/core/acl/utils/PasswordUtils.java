package com.wttech.aem.contentor.core.acl.utils;

import java.security.SecureRandom;

public class PasswordUtils {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final int MAX_LENGTH = 12;

    private PasswordUtils() {
        // intentionally empty
    }

    public static String generateRandomPassword() {
        return generateRandomPassword(MAX_LENGTH, CHARACTERS);
    }

    public static String generateRandomPassword(int length, String characterSet) {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characterSet.length());
            password.append(characterSet.charAt(index));
        }
        return password.toString();
    }
}
