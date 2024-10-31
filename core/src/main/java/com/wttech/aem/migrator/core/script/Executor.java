package com.wttech.aem.migrator.core.script;

import com.wttech.aem.migrator.core.MigratorException;
import java.time.Duration;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = Executor.class)
public class Executor {

    // TODO something like
    // https://github.com/wttech/aem-stubs/blob/main/core/src/main/java/com/wttech/aem/stubs/core/GroovyScriptStub.java#L139

    public Execution execute(Executable executable) throws MigratorException {
        return new Execution(executable, Execution.Status.SUCCESS, Duration.ofSeconds(6));
    }
}
