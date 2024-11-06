package com.wttech.aem.migrator.core.script;

import com.wttech.aem.migrator.core.MigratorException;
import com.wttech.aem.migrator.core.util.ResourceUtils;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = Executor.class)
public class Executor {

    private static final String BINDING_RESOURCE_RESOLVER = "resourceResolver";

    private static final String BINDING_OUT = "out";

    private static final String BINDING_LOG = "log";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    public Execution execute(Executable executable) throws MigratorException {
        try (var resourceResolver = ResourceUtils.serviceResolver(resourceResolverFactory)) {
            return execute(executable, new ExecutionOptions(resourceResolver, null));
        } catch (LoginException e) {
            throw new MigratorException(
                    String.format("Failed to access repository while executing '%s'", executable.getId()), e);
        }
    }

    public Execution execute(Executable executable, ExecutionOptions options) {
        var output = new StringBuilder();
        duplicateOutput(options, output);

        var shell = createShell(executable, options);
        var startTime = System.currentTimeMillis();

        try {
            var content = composeContent(executable);
            shell.evaluate(content);
            return new Execution(
                    executable, Execution.Status.SUCCESS, calculateDuration(startTime), output.toString(), null);
        } catch (Exception e) {
            return new Execution(
                    executable, Execution.Status.FAILURE, calculateDuration(startTime), output.toString(), e);
        }
    }

    private String composeContent(Executable executable) throws MigratorException {
        var builder = new StringBuilder();
        builder.append("System.setOut(new java.io.PrintStream(" + BINDING_OUT
                + ", true, java.nio.charset.StandardCharsets.UTF_8));\n");
        builder.append(executable.getContent());
        return builder.toString();
    }

    private void duplicateOutput(ExecutionOptions options, StringBuilder output) {
        var writer = new StringBuilderWriter(output);
        var stream = new WriterOutputStream(writer, StandardCharsets.UTF_8);

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
        var binding = new Binding();
        binding.setVariable(BINDING_RESOURCE_RESOLVER, options.getResourceResolver());
        binding.setVariable(BINDING_LOG, createLogger(executable));
        binding.setVariable(BINDING_OUT, options.getOutputStream());

        var compilerConfiguration = new CompilerConfiguration();
        ImportCustomizer importCustomizer = new ImportCustomizer();
        compilerConfiguration.addCompilationCustomizers(importCustomizer);

        return new GroovyShell(binding, compilerConfiguration);
    }

    private Logger createLogger(Executable executable) {
        return LoggerFactory.getLogger(String.format("%s(%s)", getClass().getSimpleName(), executable.getId()));
    }
}
