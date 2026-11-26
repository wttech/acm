package dev.vml.es.acm.core.instance;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** File that the platform creates on an instance that is about to stop. */
public class ShutdownMarkerFile {

    public enum State {
        ABSENT,
        EXISTS,
        UNKNOWN
    }

    private final String path;

    public ShutdownMarkerFile(String path) {
        this.path = path;
    }

    public State check() {
        Path file;
        try {
            file = Paths.get(path);
        } catch (InvalidPathException e) {
            return State.UNKNOWN;
        }
        if (Files.exists(file)) {
            return State.EXISTS;
        }
        if (Files.notExists(file)) {
            return State.ABSENT;
        }
        return State.UNKNOWN;
    }

    public String getPath() {
        return path;
    }
}
