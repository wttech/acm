package com.wttech.aem.acm.core.assist;

import com.wttech.aem.acm.core.code.CodeRepository;
import com.wttech.aem.acm.core.code.Variable;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class VariableSuggestion implements Suggestion {

    private final Variable variable;

    public VariableSuggestion(Variable variable) {
        this.variable = variable;
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
        return variable.varName();
    }

    @Override
    public String getInfo() {
        List<String> info = new LinkedList<>();

        info.add(String.format("Type: %s", variable.typeName()));
        CodeRepository.linkToClass(variable.typeName()).ifPresent(link -> {
            info.add(String.format("Source Code: [Open on GitHub](%s)", link));
        });

        return StringUtils.join(info, "\n\n");
    }
}
