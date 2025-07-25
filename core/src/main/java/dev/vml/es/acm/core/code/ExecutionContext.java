package dev.vml.es.acm.core.code;

import groovy.lang.Binding;
import org.slf4j.Logger;

public class ExecutionContext implements AutoCloseable {

    private final String id;

    private final ExecutionMode mode;

    private final Executor executor;

    private final Executable executable;

    private final CodeContext codeContext;

    private final CodeOutput output;

    private final CodePrintStream printStream;

    private boolean history = true;

    private boolean debug = false;

    private final Arguments arguments;

    private final Conditions conditions;

    public ExecutionContext(
            String id, ExecutionMode mode, Executor executor, Executable executable, CodeContext codeContext) {
        this.id = id;
        this.mode = mode;
        this.executor = executor;
        this.executable = executable;
        this.codeContext = codeContext;
        this.output = mode == ExecutionMode.RUN ? new CodeOutputFile(id) : new CodeOutputString();
        this.printStream = new CodePrintStream(output.write(), String.format("%s|%s", executable.getId(), id));
        this.arguments = new Arguments();
        this.conditions = new Conditions(this);

        customizeBinding();
    }

    public String getId() {
        return id;
    }

    public Executor getExecutor() {
        return executor;
    }

    public Executable getExecutable() {
        return executable;
    }

    public CodeContext getCodeContext() {
        return codeContext;
    }

    public CodeOutput getOutput() {
        return output;
    }

    public CodePrintStream getOut() {
        return getPrintStream();
    }

    public CodePrintStream getPrintStream() {
        return printStream;
    }

    public Logger getLogger() {
        return printStream.getLogger();
    }

    public ExecutionMode getMode() {
        return mode;
    }

    public boolean isHistory() {
        return history;
    }

    public void setHistory(boolean history) {
        this.history = history;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public Arguments getArguments() {
        return arguments;
    }

    public Conditions getConditions() {
        return conditions;
    }

    private void customizeBinding() {
        Binding binding = getCodeContext().getBinding();

        binding.setVariable("arguments", arguments);
        binding.setVariable("conditions", conditions);
        binding.setVariable("out", getOut());
        binding.setVariable("log", getLogger());
    }

    @Override
    public void close() {
        printStream.close();
        output.close();
    }

    public void variable(String name, Object value) {
        codeContext.getBinding().setVariable(name, value);
    }

    public Object variable(String name) {
        return codeContext.getBinding().getVariable(name);
    }
}
