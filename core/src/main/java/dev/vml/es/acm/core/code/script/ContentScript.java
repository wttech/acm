package dev.vml.es.acm.core.code.script;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.code.*;
import dev.vml.es.acm.core.code.Schedule;
import dev.vml.es.acm.core.code.schedule.BootSchedule;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;

public class ContentScript {

    private final ExecutionContext executionContext;

    private final Script script;

    public ContentScript(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.script = parseScript();
    }

    private Script parseScript() {
        GroovyShell shell = ScriptUtils.createShell(new ContentScriptSyntax());
        return shell.parse(
                executionContext.getExecutable().getContent(),
                ContentScriptSyntax.MAIN_CLASS,
                executionContext.getCodeContext().getBinding());
    }

    public Schedule schedule() {
        try {
            return (Schedule) script.invokeMethod(ContentScriptSyntax.Method.SCHEDULE.givenName, null);
        } catch (MissingMethodException e) {
            return new BootSchedule(); // intentional default schedule
        } catch (Throwable e) {
            throw new AcmException(String.format("Executable '%s' schedule method error!", executionContext.getExecutable().getId()), e);
        }
    }

    public void describe() {
        try {
            script.invokeMethod(ContentScriptSyntax.Method.DESCRIBE.givenName, null);
        } catch (MissingMethodException e) {
            // ignore as the method is optional
        } catch (Throwable e) {
            throw new AcmException(String.format("Executable '%s' describe method error!", executionContext.getExecutable().getId()), e);
        }
    }

    public boolean canRun() {
        return (Boolean) script.invokeMethod(ContentScriptSyntax.Method.CHECK.givenName, null);
    }

    public void run() {
        script.invokeMethod(ContentScriptSyntax.Method.RUN.givenName, null);
    }
}
