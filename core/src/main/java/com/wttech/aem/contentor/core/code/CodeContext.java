package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.acl.Acl;
import com.wttech.aem.contentor.core.script.Script;
import org.apache.sling.api.resource.ResourceResolver;

// TODO use '$' as the one and only binding variable in the scripts (to prevent conflicts with user-defined variables)
public class CodeContext {

    private ResourceResolver resourceResolver;

    private Acl acl;

    private Script script;

    public CodeContext(ResourceResolver resourceResolver, Acl acl, Script script) {
        this.resourceResolver = resourceResolver;
        this.acl = acl;
        this.script = script;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public Acl getAcl() {
        return acl;
    }

    public Script getScript() {
        return script;
    }
}
