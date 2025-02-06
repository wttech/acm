package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.MyAuthorizable;

public class AuthorizableOptions {

    private MyAuthorizable authorizable;

    private String id;

    public MyAuthorizable getAuthorizable() {
        return authorizable;
    }

    public void setAuthorizable(MyAuthorizable authorizable) {
        this.authorizable = authorizable;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
