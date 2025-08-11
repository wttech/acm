package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.script.ScriptRepository;
import dev.vml.es.acm.core.script.ScriptType;
import org.apache.commons.lang3.StringUtils;

public final class ExecutableUtils {

    private ExecutableUtils() {
        // intentionally empty
    }

    public static String nameById(String id) {
        if (Executable.ID_CONSOLE.equals(id)) {
            return "Console";
        }
        if (StringUtils.startsWith(id, ScriptType.AUTOMATIC.root() + "/")) {
            return String.format("Automatic Script '%s'", StringUtils.removeStart(id, ScriptType.AUTOMATIC.root() + "/"));
        }
        if (StringUtils.startsWith(id, ScriptType.MANUAL.root() + "/")) {
            return String.format("Manual Script '%s'", StringUtils.removeStart(id, ScriptType.MANUAL.root() + "/"));
        }
        if (StringUtils.startsWith(id, ScriptRepository.ROOT + "/")) {
            return String.format("Script '%s'", StringUtils.removeStart(id, ScriptRepository.ROOT + "/"));
        }
        return String.format("Executable '%s'", StringUtils.removeStart(id, AcmConstants.SETTINGS_ROOT + "/"));
    }
}
