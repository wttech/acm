package com.vml.es.aem.acm.core.instance;

import com.vml.es.aem.acm.core.AcmException;
import com.vml.es.aem.acm.core.util.ObjectUtils;
import com.vml.es.aem.acm.core.util.StreamUtils;
import java.util.Optional;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SlingInstaller.class, immediate = true)
public class SlingInstaller {

    private static final String MBEAN_NAME = "org.apache.sling.installer:type=Installer,name=Sling OSGi Installer";

    private static final String MBEAN_ACTIVE_ATTR = "Active";

    private static final String MBEAN_ACTIVE_RESOURCE_COUNT_ATTR = "ActiveResourceCount";

    private static final String PAUSE_ROOT = "/system/sling/installer/jcr/pauseInstallation";

    @Reference
    private MBeanServer mBeanServer;

    public SlingInstallerState checkState(ResourceResolver resourceResolver) {
        try {
            Set<ObjectName> mbeans = mBeanServer.queryNames(new ObjectName(MBEAN_NAME), null);
            if (mbeans.isEmpty()) {
                throw new AcmException("Cannot find Sling OSGi Installer MBean!");
            }
            ObjectName mbean = mbeans.iterator().next();

            boolean active = ObjectUtils.toBool(mBeanServer.getAttribute(mbean, MBEAN_ACTIVE_ATTR));
            long activeResourceCount =
                    ObjectUtils.toLong(mBeanServer.getAttribute(mbean, MBEAN_ACTIVE_RESOURCE_COUNT_ATTR));

            return new SlingInstallerState(active, activeResourceCount, fetchPauseCount(resourceResolver));
        } catch (Exception e) {
            throw new AcmException("Cannot check Sling Installer state!", e);
        }
    }

    private long fetchPauseCount(ResourceResolver resourceResolver) {
        return Optional.ofNullable(resourceResolver.getResource(PAUSE_ROOT))
                .map(r -> StreamUtils.asStream(r.listChildren()).count())
                .orElse(-1L);
    }
}
