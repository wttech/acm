package com.wttech.aem.contentor.core.acl;

public class UserOptions {

    private String id;

    public static UserOptions simple(String id) {
        UserOptions result = new UserOptions();
        result.id = id;
        return result;
    }
}
