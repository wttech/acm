package dev.vml.es.acm.core.assist;

import dev.vml.es.acm.core.code.CodeRepository;
import dev.vml.es.acm.core.code.Variable;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class VariableSuggestion implements Suggestion {

    private transient final Variable variable;

    private transient final CodeRepository codeRepository;

    public VariableSuggestion(Variable variable, CodeRepository codeRepository) {
        this.variable = variable;
        this.codeRepository = codeRepository;
    }

    @Override
    public String getKind() {
        return "variable";
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public String getInsertText() {
        return variable.getName();
    }

    @Override
    public String getInfo() {
        List<String> info = new LinkedList<>();

        info.add(String.format("Type: %s", variable.getType()));
        codeRepository.linkToClass(variable.getType()).ifPresent(link -> {
            info.add(String.format("Source Code: [Open](%s)", link));
        });

        return StringUtils.join(info, "\n\n");
    }
}
