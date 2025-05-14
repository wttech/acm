package com.vml.es.aem.acm.core.mock;

import com.vml.es.aem.acm.core.code.*;
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

        binding.setVariable(
                "log",
                LoggerFactory.getLogger(String.format("%s(%s)", getClass().getName(), mock.getId())));
    }
}
