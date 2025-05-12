package com.vml.es.aem.acm.core.code.script;

import com.vml.es.aem.acm.core.AcmException;
import com.vml.es.aem.acm.core.code.ExecutionContext;
import com.vml.es.aem.acm.core.mock.Mock;
import com.vml.es.aem.acm.core.mock.MockRequestException;
import com.vml.es.aem.acm.core.mock.MockResponseException;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class MockScript implements Mock {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(MockScript.class);

    private final ExecutionContext executionContext;

    private final Script script;

    public MockScript(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.script = parseScript();
    }

    private Script parseScript() {
        GroovyShell shell = ScriptUtils.createShell(new MockScriptSyntax());
        Script script = shell.parse(
                executionContext.getExecutable().getContent(),
                ContentScriptSyntax.MAIN_CLASS,
                executionContext.getBinding());
        if (script == null) {
            throw new AcmException(String.format(
                    "Mock script '%s' cannot be parsed!",
                    executionContext.getExecutable().getId()));
        }
        return script;
    }

    @Override
    public String getId() {
        return executionContext.getExecutable().getId();
    }

    @Override
    public boolean request(HttpServletRequest request) throws MockRequestException {
        try {
            LOG.info("Mock '{}' is matching request '{} {}'", getId(), request.getMethod(), request.getRequestURI());
            Boolean result = (Boolean) script.invokeMethod("request", new Object[] {request});
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

    @Override
    public void respond(HttpServletRequest request, HttpServletResponse response) throws MockResponseException {
        try {
            LOG.info(
                    "Mock '{}' is responding to request '{} {}'",
                    getId(),
                    request.getMethod(),
                    request.getRequestURI());
            script.invokeMethod("respond", new Object[] {request, response});
            LOG.info("Mock '{}' responded to request '{} {}'", getId(), request.getMethod(), request.getRequestURI());
        } catch (Exception e) {
            throw new MockResponseException(String.format("Mock script '%s' cannot respond properly", getId()), e);
        }
    }

    @Override
    public void fail(HttpServletRequest request, HttpServletResponse response, Exception exception)
            throws MockResponseException {
        try {
            LOG.info(
                    "Mock '{}' is handling failed request '{} {}'",
                    getId(),
                    request.getMethod(),
                    request.getRequestURI());
            script.invokeMethod("fail", new Object[] {request, response, exception});
            LOG.info("Mock '{}' handled failed request '{} {}'", getId(), request.getMethod(), request.getRequestURI());
        } catch (Exception e) {
            throw new MockResponseException(
                    String.format("Mock script '%s' cannot handle failed request properly", getId()), e);
        }
    }

    public String getDirPath() {
        return StringUtils.substringBeforeLast(getId(), "/");
    }

    public String resolvePath(String path) {
        if (path.startsWith("/")) {
            return path;
        }
        return getDirPath() + "/" + path;
    }
}
