package com.wttech.aem.migrator.core.script;

import com.wttech.aem.migrator.core.MigratorException;
import com.wttech.aem.migrator.core.util.ExceptionUtils;
import com.wttech.aem.migrator.core.util.ResourceUtils;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

@Component(immediate = true, service = Executor.class)
public class Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    private static final String BINDING_RESOURCE_RESOLVER = "resourceResolver";

    private static final String BINDING_OUT = "out";

    private static final String BINDING_LOG = "log";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    public Execution execute(Executable executable) throws MigratorException {
        try (ResourceResolver resourceResolver = ResourceUtils.serviceResolver(resourceResolverFactory)) {
            return execute(executable, new ExecutionOptions(resourceResolver));
        } catch (LoginException e) {
            throw new MigratorException(
                    String.format("Failed to access repository while executing '%s'", executable.getId()), e);
        }
    }

    public Execution execute(Executable executable, ExecutionOptions options) throws MigratorException {
        String id = ExecutionId.generate();
        String content = composeContent(executable);
        long startTime = System.currentTimeMillis();

        StringBuilder output = new StringBuilder();
        duplicateOutput(options, output);

        GroovyShell shell = createShell(executable, options);

        try {
            switch (options.getMode()) {
                case PARSE:
                    shell.parse(content);
                    break;
                case EVALUATE:
                    shell.evaluate(content);
                    break;
            }
            return new Execution(
                    executable, id, ExecutionStatus.SUCCEEDED, calculateDuration(startTime), output.toString(), null);
        } catch (Throwable e) {
            LOG.debug("Execution of '{}' failed! Content:\n\n{}\n\n", executable.getId(), executable.getContent(), e);
            return new Execution(
                    executable,
                    id,
                    ExecutionStatus.FAILED,
                    calculateDuration(startTime),
                    output.toString(),
                    ExceptionUtils.toString(e));
        }
    }

    private String composeContent(Executable executable) throws MigratorException {
        StringBuilder builder = new StringBuilder();
        builder.append("System.setOut(new java.io.PrintStream(" + BINDING_OUT
                + ", true, \"UTF-8\"));\n");
        builder.append(executable.getContent());
        return builder.toString();
    }

    private void duplicateOutput(ExecutionOptions options, StringBuilder output) {
        StringBuilderWriter writer = new StringBuilderWriter(output);
        WriterOutputStream stream = new WriterOutputStream(writer, StandardCharsets.UTF_8);

        if (options.getOutputStream() == null) {
            options.setOutputStream(stream);
        } else {
            options.setOutputStream(new TeeOutputStream(stream, options.getOutputStream()));
        }
    }

    private long calculateDuration(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    private GroovyShell createShell(Executable executable, ExecutionOptions options) {
        Binding binding = new Binding();
        binding.setVariable(BINDING_RESOURCE_RESOLVER, options.getResourceResolver());
        binding.setVariable(BINDING_LOG, createLogger(executable));
        binding.setVariable(BINDING_OUT, options.getOutputStream());

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        ImportCustomizer importCustomizer = new ImportCustomizer();
        compilerConfiguration.addCompilationCustomizers(importCustomizer);

        return new GroovyShell(binding, compilerConfiguration);
    }

    private Logger createLogger(Executable executable) {
        return LoggerFactory.getLogger(String.format("%s(%s)", getClass().getSimpleName(), executable.getId()));
    }
}
