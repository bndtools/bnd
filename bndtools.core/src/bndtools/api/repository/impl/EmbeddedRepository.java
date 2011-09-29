package bndtools.api.repository.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.framework.Bundle;

import aQute.lib.osgi.Instruction;
import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;
import bndtools.Plugin;
import bndtools.api.repository.RemoteRepository;
import bndtools.utils.BundleUtils;

public class EmbeddedRepository implements RemoteRepository, IExecutableExtension {

    private String name;
    private String bsn;
    private String path;

    private List<String> bsns;
    private Map<String, SortedSet<Version>> versions;
    private Bundle bundle;
    private long bundleLastModified;

    public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
        name = config.getAttribute("name");
        bsn = config.getContributor().getName();
        if(data == null || !(data instanceof String)) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Path must be specified in the configuration data", null));
        }
        path = (String) data;
    }

    public void initialise(IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor);

        bsns = new ArrayList<String>();
        versions = new HashMap<String, SortedSet<Version>>();

        bundle = BundleUtils.findBundle(Plugin.getDefault().getBundleContext(), bsn, null);
        if(bundle != null) {
            bundleLastModified = BundleUtils.getBundleLastModified(bundle);

            @SuppressWarnings("unchecked")
            Enumeration<URL> bsnEntries = bundle.findEntries(path, null, false);
            while(bsnEntries.hasMoreElements()) {
                URL bsnEntry = bsnEntries.nextElement();

                IPath bsnEntryPath = new Path(bsnEntry.getPath()).makeAbsolute();
                if(bsnEntryPath.hasTrailingSeparator()) {
                    // It's a BSN folder
                    String bsn = bsnEntryPath.lastSegment();
                    this.bsns.add(bsn);

                    @SuppressWarnings("unchecked")
                    Enumeration<URL> versionEntries = bundle.findEntries(path + "/" + bsn, null, false);
                    SortedSet<Version> versions = new TreeSet<Version>();
                    while(versionEntries.hasMoreElements()) {
                        URL versionEntry = versionEntries.nextElement();
                        IPath versionEntryPath = new Path(versionEntry.getPath()).makeAbsolute();

                        String lastSegment = versionEntryPath.lastSegment();
                        if(lastSegment.startsWith(bsn + "-") && lastSegment.toLowerCase().endsWith(".jar")) {
                            String versionStr = lastSegment.substring(bsn.length() + 1, lastSegment.length() - ".jar".length());
                            versions.add(new Version(versionStr));
                        }
                    }
                    this.versions.put(bsn, versions);
                }
            }
        }
    };

    public String getName() {
        return name;
    }

    public Collection<String> list(String regex) {
        Collection<String> result;
        if(regex == null) {
            result = Collections.unmodifiableCollection(this.bsns);
        } else {
            result = new ArrayList<String>();
            Instruction pattern = Instruction.getPattern(regex);
            for (String bsn : bsns) {
                if(pattern.matches(bsn)) result.add(bsn);
            }
        }
        return result;
    }

    public Collection<Version> versions(String bsn) {
        Collection<Version> result;

        SortedSet<Version> set = this.versions.get(bsn);
        if(set == null)
            result = Collections.emptyList();
        else
            result = Collections.unmodifiableCollection(set);
        return result;
    }

    public List<URL> get(String bsn, String range) {
        SortedSet<Version> set = this.versions.get(bsn);

        if(set == null) return Collections.emptyList();

        VersionRange  versionRange = (range == null) ?  new VersionRange("0") : new VersionRange(range);
        List<URL> result = new LinkedList<URL>();
        for (Version version : set) {
            if(versionRange.includes(version)) {
                String bundlePath = this.path + "/" + bsn + "/" + bsn + "-" + version + ".jar";

                result.add(bundle.getEntry(bundlePath));
            }
        }
        return result;
    }

    public Long getLastModified(URL url) {
        return bundleLastModified;
    }
}