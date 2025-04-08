package com.wttech.aem.acm.core.code.arg;

import com.wttech.aem.acm.core.code.Argument;
import com.wttech.aem.acm.core.code.ArgumentType;
import java.util.Calendar;

public class DateArgument extends Argument<Calendar> {
    public DateArgument(String name) {
        super(name, ArgumentType.DATE);
    }
}
