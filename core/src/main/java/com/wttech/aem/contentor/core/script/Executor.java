package com.wttech.aem.contentor.core.script;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.util.ExceptionUtils;
import com.wttech.aem.contentor.core.util.ResourceUtils;
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

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    public Execution execute(Executable executable) throws ContentorException {
        try (ResourceResolver resourceResolver = ResourceUtils.serviceResolver(resourceResolverFactory)) {
            return execute(executable, new ExecutionOptions(resourceResolver));
        } catch (LoginException e) {
            throw new ContentorException(
                    String.format("Failed to access repository while executing '%s'", executable.getId()), e);
        }
    }

    public Execution execute(Executable executable, ExecutionOptions options) throws ContentorException {
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

    private String composeContent(Executable executable) throws ContentorException {
        StringBuilder builder = new StringBuilder();
        builder.append("System.setOut(new " + Variable.OUT.typeName() + "(" + Variable.OUT.bindingName() + ", true, \"UTF-8\"));\n");
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
        binding.setVariable(Variable.RESOURCE_RESOLVER.varName, options.getResourceResolver());
        binding.setVariable(Variable.LOG.varName, createLogger(executable));
        binding.setVariable(Variable.OUT.varName, options.getOutputStream());

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        ImportCustomizer importCustomizer = new ImportCustomizer();
        compilerConfiguration.addCompilationCustomizers(importCustomizer);

        return new GroovyShell(binding, compilerConfiguration);
    }

    private Logger createLogger(Executable executable) {
        return LoggerFactory.getLogger(String.format("%s(%s)", getClass().getSimpleName(), executable.getId()));
    }
}
