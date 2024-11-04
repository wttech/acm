package com.wttech.aem.migrator.core.pkg;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.sling.event.jobs.Job;

public class PackageInstallEvent implements Serializable {

    private final LocalDateTime installedAt;

    private final PackageId packageId;

    public PackageInstallEvent(LocalDateTime installedAt, PackageId packageId) {
        this.installedAt = installedAt;
        this.packageId = packageId;
    }

    public static PackageInstallEvent fromJob(Job job) {
        return new PackageInstallEvent(
                LocalDateTime.parse(
                        job.getProperty("installedAt", String.class), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                PackageId.fromString(job.getProperty("packageId", String.class)));
    }

    public LocalDateTime getInstalledAt() {
        return installedAt;
    }

    public PackageId getPackageId() {
        return packageId;
    }

    public Map<String, Object> toJobProps() {
        var props = new HashMap<String, Object>();
        props.put("installedAt", installedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        props.put("packageId", packageId.toString());
        return props;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("installedAt", installedAt)
                .append("packageId", packageId)
                .toString();
    }
}
