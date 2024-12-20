package com.wttech.aem.contentor.core.assist;

import com.wttech.aem.contentor.core.osgi.ClassInfo;

public class ClassSuggestion implements Suggestion {

    private final ClassInfo classInfo;

    public ClassSuggestion(ClassInfo classInfo) {
        this.classInfo = classInfo;
    }

    @Override
    public String getKind() {
        return "class";
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public String getInsertText() {
        return classInfo.getClassName();
    }

    @Override
    public String getInfo() {
        return String.format("Bundle: %s", classInfo.getBundle().getSymbolicName());
    }
}
