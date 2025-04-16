package com.vml.es.aem.acm.core.acl;

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
import java.util.List;
import java.util.stream.Collectors;

@AnalyzeClasses(packages = "com.vml.es.aem.acm.core.acl")
public class ArchUnitTest {

    @ArchTest
    static final ArchRule closureMethodsShouldBeBeforeOthers = ArchRuleDefinition.classes()
            .should(new ArchCondition<JavaClass>("ensure methods with Closure<?> parameter appear before others") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    List<JavaMethod> methods = javaClass.getMethods().stream()
                            .filter(method -> !method.getModifiers().contains(JavaModifier.SYNTHETIC))
                            .sorted(Comparator.comparingInt(
                                    method -> method.getSourceCodeLocation().getLineNumber()))
                            .collect(Collectors.toList());
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
                                            "Method %s with Closure<?> parameter should be before others in class %s",
                                            method.getName(), javaClass.getName())));
                        }
                    }
                }
            });

    @ArchTest
    static final ArchRule closureMethodsShouldHaveOnlyOneParameter = ArchRuleDefinition.classes()
            .should(new ArchCondition<JavaClass>("ensure methods with Closure<?> parameter has only one parameter") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    List<JavaMethod> methods = javaClass.getMethods().stream()
                            .filter(method -> !method.getModifiers().contains(JavaModifier.SYNTHETIC))
                            .sorted(Comparator.comparingInt(
                                    method -> method.getSourceCodeLocation().getLineNumber()))
                            .collect(Collectors.toList());
                    for (JavaMethod method : methods) {
                        boolean hasClosureParam = method.getParameters().stream()
                                .map(JavaParameter::getRawType)
                                .anyMatch(type -> type.isAssignableTo(Closure.class));
                        if (hasClosureParam && method.getParameters().size() > 1) {
                            events.add(SimpleConditionEvent.violated(
                                    javaClass,
                                    String.format(
                                            "Method %s with Closure<?> parameter should have only one parameter in class %s",
                                            method.getName(), javaClass.getName())));
                        }
                    }
                }
            });
}
