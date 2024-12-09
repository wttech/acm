package com.wttech.aem.contentor.core.assist;

import com.wttech.aem.contentor.core.code.Variable;

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
        return String.format("Type: %s", variable.typeName());
    }
}
