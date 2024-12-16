package com.wttech.aem.contentor.core.code;

public class Condition {

  private final ExecutionContext executionContext;

  private final ExecutionHistory executionHistory;

  public Condition(ExecutionContext executionContext) {
    this.executionContext = executionContext;
    this.executionHistory = new ExecutionHistory(executionContext.getResourceResolver());
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
    return !executionHistory.contains(executionContext.getExecutable().getId());
  }

  // TODO check path only
  public boolean oncePerExecutableId() {
    return !executionHistory.contains(executionContext.getExecutable().getId());
  }

  // TODO check content only
  public boolean oncePerExecutableContent() {
    return false; // TOOD ...
  }

  public boolean daily() {
    // TODO check if history contains entry with this executable for today
    return false;
  }
}
