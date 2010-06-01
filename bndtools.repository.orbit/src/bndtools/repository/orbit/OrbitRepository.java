package bndtools.repository.orbit;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import aQute.lib.osgi.Instruction;
import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;
import bndtools.api.repository.RemoteRepository;
import bndtools.types.ComparablePair;

public class OrbitRepository implements RemoteRepository, IExecutableExtension {

    private URL url = null;
    private LinkedHashMap<String, SortedSet<ComparablePair<Version, URL>>> map;

    public void initialise(IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, "Initialising repository...", 20);

        if(url == null)
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Orbit repository URL not configured.", null));

        map = new LinkedHashMap<String, SortedSet<ComparablePair<Version, URL>>>();

        List<Map<String, String>> bundleEntries = null;
        try {
            InputStream stream = url.openStream();
            progress.worked(5);
            OrbitGetMapParser parser = new OrbitGetMapParser(stream);
            bundleEntries = parser.parse();
            progress.worked(5);
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Error reading Orbit repository map.", e));
        }

        if(bundleEntries != null) {
            progress.setWorkRemaining(bundleEntries.size());
            for (Map<String, String> entry : bundleEntries) {
                String bsn = entry.get(OrbitGetMapParser.PROP_BSN);
                String version = entry.get(OrbitGetMapParser.PROP_VERSION);
                String url = entry.get(OrbitGetMapParser.PROP_URL);
                try {
                    ComparablePair<Version, URL> pair = new ComparablePair<Version, URL>(new Version(version), new URL(url));

                    SortedSet<ComparablePair<Version, URL>> versionSet = map.get(bsn);
                    if(versionSet == null) {
                        versionSet = new TreeSet<ComparablePair<Version, URL>>();
                        map.put(bsn, versionSet);
                    }
                    versionSet.add(pair);
                } catch (MalformedURLException e) {
                    // Ignore this entry
                }
                progress.worked(1);
            }
        }
    }

    IProxyData[] getProxy(URI uri) {
        BundleContext context = Activator.context;
        if(context != null) {
            ServiceReference ref = context.getServiceReference(IProxyService.class.getName());
            if(ref != null) {
                IProxyService proxySvc = (IProxyService) context.getService(ref);
                if(proxySvc != null) {
                    try {
                        return proxySvc.select(uri);
                    } finally {
                        context.ungetService(ref);
                    }
                }
            }
        }
        return new IProxyData[0];
    }

    Proxy convertProxyData(IProxyData[] proxyData) {
        Proxy proxy = Proxy.NO_PROXY;

        if(proxyData != null) for (IProxyData datum : proxyData) {
            String host = datum.getHost();
            if(host != null) {
                Type type = (IProxyData.SOCKS_PROXY_TYPE.equals(datum.getType())) ? Type.SOCKS : Type.HTTP;
                //proxy = new Proxy(type, new InetSocketAddress(host, datum.getPort());
            }
        }

        return proxy;
    }

    public String getName() {
        return "Eclipse Orbit";
    }

    public Collection<String> list(String regex) {
        Collection<String> result;
        if(regex == null) {
            result = map.keySet();
        } else {
            Instruction pattern = Instruction.getPattern(regex);
            result = new LinkedList<String>();
            for (String bsn : map.keySet()) {
                if(pattern.matches(bsn)) {
                    result.add(bsn);
                }
            }
        }
        return result;
    }

    public URL[] get(String bsn, String rangeStr) {
        SortedSet<ComparablePair<Version, URL>> set = map.get(bsn);

        VersionRange range = (rangeStr == null)
            ? new VersionRange("0")
            : new VersionRange(rangeStr);

        Version low = range.getLow();
        if(!range.includeLow()) low = successor(low);
        Version high = range.isRange() ? range.getHigh() : null;
        if(high != null) {
            if(range.includeHigh()) high = successor(high);
            set = set.subSet(new ComparablePair<Version, URL>(low, null), new ComparablePair<Version, URL>(high, null));
        } else {
            set = set.tailSet(new ComparablePair<Version, URL>(low, null));
        }

        URL[] result = new URL[set.size()];
        int i=0;
        for(ComparablePair<Version, URL> pair : set) {
            result[i] = pair.getSecond();
            i++;
        }

        return result;
    }

    private Version successor(Version version) {
        String qualifier = version.getQualifier();
        if(qualifier == null) {
            qualifier = "\0";
        } else {
            qualifier = qualifier + "\0";
        }
        return new Version(version.getMajor(), version.getMinor(), version.getMicro(), qualifier);
    }

    public Collection<Version> versions(String bsn) {
        SortedSet<ComparablePair<Version, URL>> set = map.get(bsn);
        if(set == null) return null;
        List<Version> result = new ArrayList<Version>(set.size());
        for (ComparablePair<Version, URL> pair : set) {
            result.add(pair.getFirst());
        }
        return Collections.unmodifiableList(result);
    }

    public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
        if(data == null || !(data instanceof String))
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Orbit repository URL not configured.", null));
        String urlStr = (String) data;
        try {
            setURL(new URL(urlStr));
        } catch (MalformedURLException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, MessageFormat.format("Invalid Orbit repository URL: {0}.", urlStr), e));
        }
    }

    // For testing access
    void setURL(URL url) {
        this.url = url;
    }
}