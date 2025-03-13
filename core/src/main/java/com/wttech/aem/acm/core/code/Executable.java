package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.AcmException;
import java.io.Serializable;

public interface Executable extends Serializable {

    String getId();

    String getContent() throws AcmException;
}
