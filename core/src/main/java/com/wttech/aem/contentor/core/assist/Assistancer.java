package com.wttech.aem.contentor.core.assist;

import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = Assistancer.class)
public class Assistancer {

    public Assistance forWord(String word) {
        return Assistance.mock(word);
    }
}
