package com.wttech.aem.contentor.core.servlet;

import com.wttech.aem.contentor.core.script.Script;
import java.io.Serializable;
import java.util.List;

public class ScriptOutput implements Serializable {

    public List<Script> list;

    public ScriptOutput(List<Script> scripts) {
        this.list = scripts;
    }

    public List<Script> getList() {
        return list;
    }
}
