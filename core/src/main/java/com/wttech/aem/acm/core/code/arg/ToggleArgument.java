package com.wttech.aem.acm.core.code.arg;

import com.wttech.aem.acm.core.code.Argument;
import com.wttech.aem.acm.core.code.ArgumentType;

public class ToggleArgument extends Argument<Boolean> {

    public ToggleArgument(String name) {
        super(name, ArgumentType.TOGGLE);
    }
}
