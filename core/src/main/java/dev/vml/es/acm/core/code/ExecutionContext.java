package dev.vml.es.acm.core.code;

import groovy.lang.Binding;

public class ExecutionContext implements AutoCloseable {

    private final String id;

    private final ExecutionMode mode;

    private final Executor executor;

    private final Executable executable;

    private final CodeContext codeContext;

    private final CodeOutput codeOutput;

    private final CodePrintStream codePrintStream;

    private boolean history = true;

    private boolean debug = false;

    private final Arguments arguments;

    private final Condition condition;

    public ExecutionContext(
            String id, ExecutionMode mode, Executor executor, Executable executable, CodeContext codeContext) {
        this.id = id;
        this.mode = mode;
        this.executor = executor;
        this.executable = executable;
        this.codeContext = codeContext;
        this.codeOutput = mode == ExecutionMode.RUN ? new CodeOutputFile(id) : new CodeOutputString();
        this.codePrintStream = new CodePrintStream(String.format("%s|%s", executable.getId(), id), codeOutput.write());
        this.arguments = new Arguments();
        this.condition = new Condition(this);

        customizeBinding();
    }

    public String getId() {
        return id;
    }

    public CodeOutput getOutput() {
        return codeOutput;
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

    public Condition getCondition() {
        return condition;
    }

    private void customizeBinding() {
        Binding binding = getCodeContext().getBinding();

        binding.setVariable("arguments", arguments);
        binding.setVariable("condition", condition);
        binding.setVariable("out", codePrintStream);
        binding.setVariable("log", codePrintStream.getLogger());
    }

    @Override
    public void close() {
        codePrintStream.close();
        codeOutput.close();
    }

    public void variable(String name, Object value) {
        codeContext.getBinding().setVariable(name, value);
    }
}
