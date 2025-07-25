package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.script.ScriptRepository;
import org.apache.commons.lang3.StringUtils;

public final class ExecutableUtils {

    private ExecutableUtils() {
        // intentionally empty
    }

    public static String nameById(String id) {
        if (Executable.ID_CONSOLE.equals(id)) {
            return "Console";
        }
        if (StringUtils.startsWith(id, ScriptRepository.ROOT)) {
            return String.format("Script '%s'", StringUtils.removeStart(id, ScriptRepository.ROOT + "/"));
        }
        return String.format("Executable '%s'", StringUtils.removeStart(id, AcmConstants.SETTINGS_ROOT + "/"));
    }
}
