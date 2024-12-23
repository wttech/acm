package com.wttech.aem.contentor.core.checkacl;

import java.util.List;

public class ExcludeOptions {

    private String group;

    private String id;

    private List<String> ids;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }
}
