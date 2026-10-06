package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.util.ChecksumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trail of code run through the ACM API. Only runs that leave no execution in history are logged at INFO;
 * everything else (compile checks, describing inputs, recorded runs) at DEBUG, so busy clients do not flood logs.
 */
public final class ExecutionAudit {

    public static final String LOGGER_NAME = "dev.vml.es.acm.audit";

    private static final Logger LOG = LoggerFactory.getLogger(LOGGER_NAME);

    private static final String MESSAGE =
            "{} by '{}' | executable '{}', mode {}, history {}, status {}, execution '{}', content checksum '{}'";

    private ExecutionAudit() {
        // intentionally empty
    }

    public static void log(String action, ExecutionContext context, Execution execution) {
        LOG.info(MESSAGE, args(action, context, execution));
    }

    public static void trace(String action, ExecutionContext context, Execution execution) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(MESSAGE, args(action, context, execution));
        }
    }

    private static Object[] args(String action, ExecutionContext context, Execution execution) {
        return new Object[] {
            action,
            context.getUserId(),
            context.getExecutable().getId(),
            context.getMode(),
            context.isHistory(),
            execution.getStatus(),
            execution.getId(),
            ChecksumUtils.calculate(context.getExecutable().getContent())
        };
    }
}
