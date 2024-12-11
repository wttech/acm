package com.wttech.aem.contentor.core.code;

public class Condition {

    private final ExecutionContext executionContext;

    public Condition(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public boolean always() {
        return true;
    }

    public boolean never() {
        return false;
    }

    public boolean once() {
        return oncePerExecutableContent();
    }

    // TODO check content and path
    public boolean oncePerExecutable() {
        return !executionContext.getHistory().contains(executionContext.getExecutable().getId());
    }

    // TODO check path only
    public boolean oncePerExecutableId() {
        return !executionContext.getHistory().contains(executionContext.getExecutable().getId());
    }

    // TODO check content only
    public boolean oncePerExecutableContent() {
        return false; // TOOD ...
    }
}
