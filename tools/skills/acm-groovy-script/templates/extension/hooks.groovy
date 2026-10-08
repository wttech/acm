/*
---
version: '1.0'
category: extension
---
Hooks into every execution, e.g. to share helpers or react to failures.

Provides the `acme` helper to all scripts and logs failed executions. Save it as `main.groovy` in the project folder.
*/

import dev.vml.es.acm.core.code.Execution
import dev.vml.es.acm.core.code.ExecutionContext

void prepareRun(ExecutionContext context) {
    context.variable('acme', new AcmeHelper())
}

void completeRun(Execution execution) {
    if (execution.status.name() == 'FAILED') {
        log.error "Script '${execution.executable.id}' failed"
    }
}

class AcmeHelper {}
