package com.wttech.aem.contentor.core.assist;

import com.wttech.aem.contentor.core.code.CodeRepository;
import com.wttech.aem.contentor.core.osgi.ClassInfo;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

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
        List<String> info = new LinkedList<>();

        info.add(String.format("Bundle: %s", classInfo.getBundle().getSymbolicName()));
        CodeRepository.linkToClass(classInfo.getClassName()).ifPresent(link -> {
            info.add(String.format("Source Code: [Open on GitHub](%s)", link));
        });

        return StringUtils.join(info, "\n\n");
    }
}
