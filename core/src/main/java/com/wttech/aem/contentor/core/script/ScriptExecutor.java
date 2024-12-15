package com.wttech.aem.contentor.core.script;

import com.wttech.aem.contentor.core.code.Execution;
import com.wttech.aem.contentor.core.code.Executor;
import com.wttech.aem.contentor.core.util.ResourceUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = {Runnable.class})
@Designate(ocd = ScriptExecutor.Config.class)
public class ScriptExecutor implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptExecutor.class);

    @ObjectClassDefinition(name = "AEM Contentor - Script Executor")
    public @interface Config {

        @AttributeDefinition(
                name = "Cron Expression",
                description = "Determines how often the scripts should be executed",
                defaultValue = "0/30 * * * * ?"
        )
        String scheduler_expression() default "0/30 * * * * ?";
    }

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Executor executor;

    @Override
    public void run() {
        try (ResourceResolver resourceResolver = ResourceUtils.serviceResolver(resourceResolverFactory)) {
            ScriptRepository scriptRepository = new ScriptRepository(resourceResolver);

            LOG.info("Executing scripts");
            scriptRepository.findAll(ScriptType.ENABLED).forEach(script -> {
                try {
                    Execution execution = executor.execute(script);
                    LOG.info("Execution of script '{}' ended with result '{}'", script.getId(), execution);
                } catch (Exception e) {
                    LOG.error("Failed to execute script '{}'", script.getId(), e);
                }
            });
            LOG.info("Executed scripts");
        } catch (Exception e) {
            LOG.error("Failed to execute scripts", e);
        }
    }
}
