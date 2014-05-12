package org.bndtools.core.resolve.ui;

import org.bndtools.utils.resources.ResourceUtils;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

class BundleSorter extends ViewerSorter {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        Resource r1 = (Resource) e1;
        Resource r2 = (Resource) e2;

        Capability id1 = ResourceUtils.getIdentityCapability(r1);
        Capability id2 = ResourceUtils.getIdentityCapability(r2);

        String name1 = ResourceUtils.getIdentity(id1);
        if (name1 == null) {
            name1 = "";
        }
        String name2 = ResourceUtils.getIdentity(id2);
        if (name2 == null) {
            name2 = "";
        }

        int ret = name1.compareTo(name2);
        if (ret != 0) {
            return ret;
        }

        Version ver1 = ResourceUtils.getVersion(id1);
        if (ver1 == null) {
            ver1 = Version.emptyVersion;
        }
        Version ver2 = ResourceUtils.getVersion(id2);
        if (ver2 == null) {
            ver2 = Version.emptyVersion;
        }
        return ver1.compareTo(ver2);
    }
}