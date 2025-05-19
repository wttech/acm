package dev.vml.es.acm.core.osgi;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.osgi.framework.Bundle;

public class ClassInfo implements Comparable<ClassInfo> {

    private final String className;

    private final Bundle bundle;

    public ClassInfo(String className, Bundle bundle) {
        this.className = className;
        this.bundle = bundle;
    }

    public String getClassName() {
        return className;
    }

    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public int compareTo(ClassInfo other) {
        return this.className.compareTo(other.className);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassInfo classInfo = (ClassInfo) o;
        return new EqualsBuilder()
                .append(className, classInfo.className)
                .append(bundle.getSymbolicName(), classInfo.bundle.getSymbolicName())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(className)
                .append(bundle.getSymbolicName())
                .toHashCode();
    }
}
