package com.vml.es.aem.acm.core.code;

import com.vml.es.aem.acm.core.AcmException;
import java.io.Serializable;

public interface Executable extends Serializable {

    String getId();

    String getContent() throws AcmException;

    ArgumentValues getArguments();
}
