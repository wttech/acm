package dev.vml.es.acm.core.assist;

import dev.vml.es.acm.core.code.CodeRepository;
import dev.vml.es.acm.core.osgi.ClassInfo;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class ClassSuggestion implements Suggestion {

    private final ClassInfo classInfo;

    private final CodeRepository codeRepository;

    public ClassSuggestion(ClassInfo classInfo, CodeRepository codeRepository) {
        this.classInfo = classInfo;
        this.codeRepository = codeRepository;
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
        codeRepository.linkToClass(classInfo).ifPresent(link -> {
            info.add(String.format("Documentation: [Open](%s)", link));
        });

        return StringUtils.join(info, "\n\n");
    }
}
