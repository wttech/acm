package com.vml.es.aem.acm.core.code.script;

import com.vml.es.aem.acm.core.AcmException;
import com.vml.es.aem.acm.core.mock.MockContext;
import com.vml.es.aem.acm.core.mock.MockRequestException;
import com.vml.es.aem.acm.core.mock.MockResponseException;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;

public class MockScript {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(MockScript.class);

    private final MockContext context;

    private final MockType type;

    private final Script script;

    public MockScript(MockContext context, MockType type) {
        this.context = context;
        this.type = type;
        this.script = parseScript();
    }

    private Script parseScript() {
        GroovyShell shell = ScriptUtils.createShell(new MockScriptSyntax(type));
        Script script = shell.parse(
                context.getMock().getContent(),
                MockScriptSyntax.MAIN_CLASS,
                context.getCodeContext().getBinding());
        if (script == null) {
            throw new AcmException(String.format(
                    "Mock script '%s' cannot be parsed!", context.getMock().getId()));
        }
        return script;
    }

    public boolean request(HttpServletRequest request) throws MockRequestException {
        try {
            LOG.info("Mock '{}' is matching request '{} {}'", getId(), request.getMethod(), request.getRequestURI());
            Boolean result =
                    (Boolean) script.invokeMethod(MockScriptSyntax.Method.REQUEST.givenName, new Object[] {request});
            if (BooleanUtils.isTrue(result)) {
                LOG.info("Mock '{}' matched request '{} {}'", getId(), request.getMethod(), request.getRequestURI());
            } else {
                LOG.info(
                        "Mock '{}' did not match request '{} {}'",
                        getId(),
                        request.getMethod(),
                        request.getRequestURI());
            }
            return result;
        } catch (Exception e) {
            throw new MockRequestException(String.format("Mock script '%s' cannot match request properly", getId()), e);
        }
    }

    public void respond(HttpServletRequest request, HttpServletResponse response) throws MockResponseException {
        try {
            LOG.info(
                    "Mock '{}' is responding to request '{} {}'",
                    getId(),
                    request.getMethod(),
                    request.getRequestURI());
            script.invokeMethod(MockScriptSyntax.Method.RESPOND.givenName, new Object[] {request, response});
            LOG.info("Mock '{}' responded to request '{} {}'", getId(), request.getMethod(), request.getRequestURI());
        } catch (Exception e) {
            throw new MockResponseException(String.format("Mock script '%s' cannot respond properly", getId()), e);
        }
    }

    public void fail(HttpServletRequest request, HttpServletResponse response, Exception exception)
            throws MockResponseException {
        try {
            LOG.info(
                    "Mock '{}' is handling failed request '{} {}'",
                    getId(),
                    request.getMethod(),
                    request.getRequestURI());
            script.invokeMethod(MockScriptSyntax.Method.FAIL.givenName, new Object[] {request, response, exception});
            LOG.info("Mock '{}' handled failed request '{} {}'", getId(), request.getMethod(), request.getRequestURI());
        } catch (Exception e) {
            throw new MockResponseException(
                    String.format("Mock script '%s' cannot handle failed request properly", getId()), e);
        }
    }

    public String getId() {
        return context.getMock().getId();
    }
}
