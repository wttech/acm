package dev.vml.es.acm.core.state;

import java.io.Serializable;

public class Permissions implements Serializable {

    private boolean console;

    public Permissions(boolean console) {
        this.console = console;
    }
    
    public boolean isConsole() {
        return console;
    }
}
