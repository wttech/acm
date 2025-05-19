package dev.vml.es.acm.core.mock;

import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.repo.RepoResource;
import groovy.lang.Binding;
import org.slf4j.LoggerFactory;

public class MockContext {

    private final CodeContext codeContext;

    private final Mock mock;

    public MockContext(CodeContext codeContext, Mock mock) {
        this.codeContext = codeContext;
        this.mock = mock;

        customizeBinding();
    }

    public CodeContext getCodeContext() {
        return codeContext;
    }

    public Mock getMock() {
        return mock;
    }

    public void variable(String name, Object value) {
        codeContext.getBinding().setVariable(name, value);
    }

    private void customizeBinding() {
        Binding binding = getCodeContext().getBinding();

        binding.setVariable("mock", this);
        binding.setVariable(
                "log",
                LoggerFactory.getLogger(String.format("%s(%s)", getClass().getName(), mock.getId())));
    }

    public RepoResource getResource() {
        return RepoResource.of(codeContext.getResourceResolver(), mock.getPath());
    }
}
