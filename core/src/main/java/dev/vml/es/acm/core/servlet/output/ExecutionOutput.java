package dev.vml.es.acm.core.servlet.output;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ExecutionOutput implements Serializable {

    public enum Name {
        ARCHIVE("acm-archive"),
        CONSOLE("acm-console");

        private final String id;

        Name(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Optional<Name> byId(String name) {
            return Arrays.stream(values())
                    .filter(n -> n.id().equalsIgnoreCase(name))
                    .findFirst();
        }
    }

    @SuppressWarnings("java:S1948")
    private List<?> list;

    public ExecutionOutput(List<?> executions) {
        this.list = executions;
    }

    public List<?> getList() {
        return list;
    }
}
