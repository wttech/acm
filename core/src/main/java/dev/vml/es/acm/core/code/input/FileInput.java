package dev.vml.es.acm.core.code.input;

import dev.vml.es.acm.core.code.InputType;

public class FileInput extends AbstractFileInput<String> {

    public FileInput(String name) {
        super(name, InputType.FILE, String.class);
    }
}
