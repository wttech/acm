package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import java.io.Serializable;

public interface Executable extends Serializable {

    String CONSOLE_ID = "console";

    String CONSOLE_SCRIPT_PATH = "/conf/acm/settings/script/template/core/console.groovy";

    String getId();

    String getContent() throws AcmException;
}
