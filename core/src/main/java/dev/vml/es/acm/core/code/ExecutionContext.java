package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.util.ResolverUtils;
import groovy.lang.Binding;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;

public class ExecutionContext implements AutoCloseable {

    public static final String VAR_ROOT = AcmConstants.VAR_ROOT + "/execution/context";

    public static String varPath(String executionId) {
        return String.format("%s/%s", VAR_ROOT, StringUtils.replace(executionId, "/", "-"));
    }

    private final String id;

    private final String userId;

    private final ExecutionMode mode;

    private final Executor executor;

    private final Executable executable;

    private final CodeContext codeContext;

    private final CodeOutput output;

    private final CodePrintStream printStream;

    private boolean history = true;

    private boolean debug = false;

    private boolean locking = true;

    private final Inputs inputs;

    private InputValues inputValues;

    private final Outputs outputs;

    private final Schedules schedules;

    private final Conditions conditions;

    public ExecutionContext(
            String id,
            String userId,
            ExecutionMode mode,
            Executor executor,
            Executable executable,
            InputValues inputValues,
            CodeContext codeContext) {
        this.id = id;
        this.userId = userId;
        this.mode = mode;
        this.executor = executor;
        this.executable = executable;
        this.inputValues = inputValues;
        this.codeContext = codeContext;
        this.output = determineOutput(mode, codeContext, id);
        this.printStream = new CodePrintStream(output.write(), String.format("%s|%s", executable.getId(), id));
        this.schedules = new Schedules();
        this.conditions = new Conditions(this);
        this.inputs = new Inputs();
        this.outputs = new Outputs(this);

        customizeBinding();
    }

    private CodeOutput determineOutput(ExecutionMode mode, CodeContext codeContext, String id) {
        ResourceResolverFactory resolverFactory =
                codeContext.getOsgiContext().getService(ResourceResolverFactory.class);
        SpaSettings spaSettings = codeContext.getOsgiContext().getService(SpaSettings.class);
        return mode == ExecutionMode.RUN
                ? new CodeOutputRepo(resolverFactory, spaSettings, id)
                : new CodeOutputMemory();
    }

    private void cleanOutputs() {
        ResourceResolverFactory resolverFactory =
                codeContext.getOsgiContext().getService(ResourceResolverFactory.class);
        ResolverUtils.useContentResolver(resolverFactory, null, resolver -> {
            Repo.quiet(resolver).get(ExecutionContext.varPath(getId())).delete();
        });
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
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

    public boolean isLocking() {
        return locking;
    }

    public void setLocking(boolean locking) {
        this.locking = locking;
    }

    public Inputs getInputs() {
        return inputs;
    }

    void useInputValues() {
        inputs.setValues(inputValues);
    }

    public Outputs getOutputs() {
        return outputs;
    }

    public Schedules getSchedules() {
        return schedules;
    }

    public Conditions getConditions() {
        return conditions;
    }

    private void customizeBinding() {
        Binding binding = getCodeContext().getBinding();

        binding.setVariable("schedules", schedules);
        binding.setVariable("arguments", inputs); // TODO deprecated
        binding.setVariable("inputs", inputs);
        binding.setVariable("outputs", outputs);
        binding.setVariable("conditions", conditions);
        binding.setVariable("out", getOut());
        binding.setVariable("log", getLogger());
    }

    @Override
    public void close() {
        printStream.close();
        output.close();
        outputs.close();
        cleanOutputs();
    }

    public void variable(String name, Object value) {
        codeContext.getBinding().setVariable(name, value);
    }

    public Object variable(String name) {
        return codeContext.getBinding().getVariable(name);
    }
}
