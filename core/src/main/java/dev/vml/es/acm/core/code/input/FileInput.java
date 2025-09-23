package dev.vml.es.acm.core.code.input;

import dev.vml.es.acm.core.code.InputType;
import java.io.File;

public class FileInput extends AbstractFileInput<File> {

    public FileInput(String name) {
        super(name, InputType.FILE, File.class);
    }
}
