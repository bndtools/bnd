package bndtools;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
import org.ardverk.collection.Trie;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Clazz;
import aQute.lib.osgi.Jar;
import bndtools.types.Pair;

public class RepositoryModel {

    private Trie<String, Map<File, BundleInfo>> nameLookup = new PatriciaTrie<String, Map<File, BundleInfo>>(new StringKeyAnalyzer());
    private Map<File, Set<String>> reverseLookup = new HashMap<File, Set<String>>();

    public List<Pair<Clazz, BundleInfo>> findMatches(String prefix) {
        List<Pair<Clazz, BundleInfo>> result = new LinkedList<Pair<Clazz,BundleInfo>>();

        SortedMap<String,Map<File,BundleInfo>> map = nameLookup.getPrefixedBy(prefix);
        if(map != null) for (Entry<String, Map<File, BundleInfo>> entry : map.entrySet()) {
            String shortName = entry.getKey();
            Map<File, BundleInfo> fileMap = entry.getValue();
            for (Entry<File, BundleInfo> fileEntry : fileMap.entrySet()) {
                BundleInfo bundleInfo = fileEntry.getValue();
                List<Clazz> list = bundleInfo.clazzes.get(shortName);
                for (Clazz clazz : list) {
                    Pair<Clazz, BundleInfo> pair = new Pair<Clazz, BundleInfo>(clazz, bundleInfo);
                    result.add(pair);
                }
            }
        }

        return result;
    }

    private synchronized void insertBundleInfo(BundleInfo info) {
        for (Entry<String, List<Clazz>> entry : info.clazzes.entrySet()) {
            String shortName = entry.getKey();

            // Update the classname map
            Map<File, BundleInfo> map = nameLookup.get(shortName);
            if(map == null) {
                map = new HashMap<File, BundleInfo>();
                nameLookup.put(shortName, map);
            }
            map.put(info.file, info);

            // Update the reverse lookup map
            Set<String> nameSet = reverseLookup.get(info.file);
            if(nameSet == null) {
                nameSet = new HashSet<String>();
                reverseLookup.put(info.file, nameSet);
            }
            nameSet.add(shortName);
        }
    }

    /**
     *
     * @param repo
     * @param file
     * @param monitor
     *            the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call done() on the
     *            given monitor. Accepts null, indicating that no progress
     *            should be reported and that the operation cannot be cancelled.
     *            * @throws IOException
     * @throws CoreException
     */
    public void updateRepositoryBundle(RepositoryPlugin repo, File file, IProgressMonitor monitor) throws IOException, CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, file.getName(), 1);

        Collection<Clazz> clazzes = analyseBundleFile(file);
        int size = clazzes != null ? clazzes.size() : 0;
    }

    Collection<Clazz> analyseBundleFile(File file) throws IOException, CoreException {
        Builder builder = new Builder();
        Jar jar = new Jar(file);
        builder.setJar(jar);

        try {
            builder.analyze();
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to analyse bundle \"{0}\".", file.getPath()), e));
        } finally {
            jar.close();
        }

        String bsn = builder.getBsn();
        String version = builder.getVersion();

        Collection<Clazz> clazzes = null;
        try {
            clazzes = builder.getClasses("classes", "PUBLIC");
            if(clazzes != null) {
                Map<String, List<Clazz>> classMap = new HashMap<String, List<Clazz>>();
                for (Clazz clazz : clazzes) {
                    String shortName = clazz.getClassName().getShortName();

                    List<Clazz> list = classMap.get(shortName);
                    if(list == null) {
                        list = new LinkedList<Clazz>();
                        classMap.put(shortName, list);
                    }
                    list.add(clazz);
                }

                BundleInfo bundleInfo = new BundleInfo(bsn, version, file, classMap);
                insertBundleInfo(bundleInfo);
            }
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to analyse public classes in bundle \"{0}\".", file.getPath()), e));
        }
        return clazzes;
    }
}