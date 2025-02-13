package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.acl.authorizable.MyGroup;
import com.wttech.aem.contentor.core.acl.authorizable.MyUser;

public class DeleteGroupOptions {

  private MyGroup group;

  private String id;

  public MyGroup getGroup() {
    return group;
  }

  public void setGroup(MyGroup group) {
    this.group = group;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
