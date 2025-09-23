package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import java.io.Serializable;

public interface Executable extends Serializable {

    String ID_CONSOLE = "console";

    String getId();

    String getContent() throws AcmException;

    InputValues getInputs();
}
