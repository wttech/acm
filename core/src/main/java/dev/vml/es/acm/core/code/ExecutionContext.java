package dev.vml.es.acm.core.code;

import groovy.lang.Binding;
import org.slf4j.LoggerFactory;

public class ExecutionContext implements AutoCloseable {

    private final String id;

    private final ExecutionMode mode;

    private final Output output;

    private final Executor executor;

    private final Executable executable;

    private final CodeContext codeContext;

    private boolean history = true;

    private boolean debug = false;

    private final Arguments arguments = new Arguments();

    public ExecutionContext(
            String id, ExecutionMode mode, Executor executor, Executable executable, CodeContext codeContext) {
        this.id = id;
        this.mode = mode;
        this.output = mode == ExecutionMode.RUN ? new OutputFile(id) : new OutputString();
        this.executor = executor;
        this.executable = executable;
        this.codeContext = codeContext;

        customizeBinding();
    }

    public String getId() {
        return id;
    }

    public Output getOutput() {
        return output;
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

    private void customizeBinding() {
        Binding binding = getCodeContext().getBinding();

        binding.setVariable("args", arguments);
        binding.setVariable("condition", new Condition(this));
        binding.setVariable(
                "log",
                LoggerFactory.getLogger(String.format("%s(%s)", getClass().getName(), executable.getId())));
        binding.setVariable("out", new CodePrintStream(this));
    }

    @Override
    public void close() {
        output.close();
    }

    public void variable(String name, Object value) {
        codeContext.getBinding().setVariable(name, value);
    }
}
