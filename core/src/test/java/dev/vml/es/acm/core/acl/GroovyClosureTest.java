package dev.vml.es.acm.core.acl;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import groovy.lang.Closure;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * For Java and Groovy interoperability, Groovy closures must be defined before other overloaded Java methods
 * in the class to ensure they are properly callable from Groovy scripts.
 */
@AnalyzeClasses(packages = "dev.vml.es.acm.core")
public class GroovyClosureTest {

    @ArchTest
    static final ArchRule closureMethodsShouldBeBeforeOthers = ArchRuleDefinition.classes()
            .should(
                    new ArchCondition<JavaClass>(
                            "ensure methods with Closure<?> parameter appear before others with the same name and argument count") {
                        @Override
                        public void check(JavaClass javaClass, ConditionEvents events) {
                            javaClass.getMethods().stream()
                                    .filter(method -> !method.getModifiers().contains(JavaModifier.SYNTHETIC))
                                    .collect(Collectors.groupingBy(method -> method.getName() + "#"
                                            + method.getParameters().size()))
                                    .forEach((methodSignature, methods) -> {
                                        methods.sort(Comparator.comparingInt(method ->
                                                method.getSourceCodeLocation().getLineNumber()));
                                        boolean foundNonClosureMethod = false;
                                        for (JavaMethod method : methods) {
                                            boolean hasClosureParam = method.getParameters().stream()
                                                    .map(JavaParameter::getRawType)
                                                    .anyMatch(type -> type.isAssignableTo(Closure.class));
                                            if (!hasClosureParam) {
                                                foundNonClosureMethod = true;
                                            }
                                            if (hasClosureParam && foundNonClosureMethod) {
                                                events.add(SimpleConditionEvent.violated(
                                                        javaClass,
                                                        String.format(
                                                                "Method '%s' with Closure<?> parameter should be before others with the same name and argument count in class '%s'",
                                                                method.getName(), javaClass.getName())));
                                            }
                                        }
                                    });
                        }
                    });
}
