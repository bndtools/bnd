package bndtools.bndplugins.repo.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import aQute.bnd.service.Plugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Instruction;
import aQute.lib.osgi.Jar;
import aQute.libg.reporter.Reporter;
import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;

public class EclipseRepo implements Plugin, RepositoryPlugin {

    private static final String DEFAULT_VERSION = "0";
    private File root;
    private Reporter reporter;
    private String name;

    private Map<String, Map<String, File>> index;

    public static String LOCATION = "location";
    public static String NAME = "name";


    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    public void setProperties(Map<String, String> map) {
        String location = map.get(LOCATION);
        if (location == null)
            throw new IllegalArgumentException(
                    "Location must be set on an EclipseRepo plugin");

        name = map.get(NAME);
        if (name == null || name.length() == 0)
            name = "Eclipse SDK";

        File oldRoot = root;
        root = new File(location);
        if (!root.isDirectory())
            throw new IllegalArgumentException("Repository is not a valid directory: " + root);

        if (!new File(root, "plugins").isDirectory())
            throw new IllegalArgumentException("Repository is not a valid directory (no 'plugins' sub-directory): " + root);

        if (!root.equals(oldRoot))
            index = readIndex();
    }

    private Map<String, Map<String, File>> readIndex() {
        File[] pluginFiles = new File(root, "plugins").listFiles();

        // Build the index
        Map<String, Map<String, File>> index = new HashMap<String, Map<String,File>>();
        if(pluginFiles != null) for (File pluginFile : pluginFiles) {
            // Determine the bsn and version
            BundleIdentity id;
            try {
                id = getBundleIdentity(pluginFile);
            } catch (IllegalArgumentException e) {
                if(reporter != null) reporter.error("Error adding file '%s' to index: %s", pluginFile.getName(), e.getMessage());
                continue;
            } catch (Exception e) {
                if(reporter != null) reporter.error("Error reading file '%s': %s", pluginFile.getName(), e.getMessage());
                continue;
            }

            // Skip source bundles
            if(id.getBsn().toLowerCase().endsWith(".source")) continue;

            // Put into the map
            Map<String, File> bundleMap = index.get(id.getBsn());
            if(bundleMap == null) {
                bundleMap = new HashMap<String, File>();
                index.put(id.getBsn(), bundleMap);
            }
            bundleMap.put(id.getVersion(), pluginFile);
        }

        return index;
    }

    BundleIdentity getBundleIdentity(File pluginFile) throws Exception {
        String name = pluginFile.getName();
        if(pluginFile.isFile()) {
            if(name.toLowerCase().endsWith(".jar"))
                name = name.substring(0, name.length() - ".jar".length());
            else
                throw new IllegalArgumentException("Not a directory or JAR file.");
        }

        BundleIdentity result;
        int firstUnderscore = name.indexOf('_');
        if(firstUnderscore == -1) {
            // This bundle has no version tag
            result = new BundleIdentity(name, DEFAULT_VERSION);
        } else {
            // Check if there are any more underscores...
            if(name.length() <= firstUnderscore + 1) {
                // The first underscore was at the end of the name, weird.
                result = new BundleIdentity(name.substring(0, firstUnderscore), DEFAULT_VERSION);
            } else {
                int secondUnderscore = name.indexOf('_', firstUnderscore + 1);
                if(secondUnderscore == -1) {
                    // The name only had one underscore, phew.
                    String bsn = name.substring(0, firstUnderscore);
                    String version = name.substring(firstUnderscore + 1);
                    result = new BundleIdentity(bsn, version);
                } else {
                    // The geniuses who build Eclipse decided to separate bsn from version with an
                    // underscore, and also allow underscores in both name and version. In this case
                    // we have no choice but to read the manifest.
                    Jar jar = new Jar(pluginFile);
                    try {
                        Manifest manifest = jar.getManifest();
                        Attributes attribs = manifest.getMainAttributes();
                        String bsn = attribs.getValue(Constants.BUNDLE_SYMBOLICNAME);
                        String version = attribs.getValue(Constants.BUNDLE_VERSION);

                        if(bsn == null)
                            throw new IllegalArgumentException("Jar manifest does not contain Bundle-SymbolicName; this is not a bundle.");
                        int semicolon = bsn.indexOf(';');
                        if(semicolon > -1)
                            bsn = bsn.substring(0, semicolon);
                        if(version == null)
                            version = DEFAULT_VERSION;

                        result = new BundleIdentity(bsn, version);
                    } finally {
                        jar.close();
                    }
                }
            }
        }

        return result;
    }

    public boolean canWrite() {
        return false;
    }

    public File put(Jar jar) throws Exception {
        throw new UnsupportedOperationException("#put(aQute.lib.osgi.Jar)");
    }

    public List<String> list(String regex) {
        Instruction pattern = null;
        if (regex != null)
            pattern = new Instruction(regex);

        List<String> result = new ArrayList<String>();
        for (String f : index.keySet()) {
            if (pattern == null || pattern.matches(f))
                result.add(f);
        }
        return result;
    }

    public List<Version> versions(String bsn) {
        Map<String, File> instances = index.get(bsn);
        if (instances == null)
            return null;

        List<Version> versions = new ArrayList<Version>();
        for (String v : instances.keySet())
            versions.add(new Version(v));
        return versions;
    }

    private File getSourceAugmentedFile(File pluginFile, String bsn, String version) {
        // Don't merge source for a directory-based plugin.
        if(pluginFile.isDirectory()) return null;

        // Get the source plugin
        File pluginsDir = new File(root, "plugins");
        File sourceFile = new File(pluginsDir, bsn + ".source_" + version + ".jar");
        if(!sourceFile.exists()) return null;

        // Get the cached augmented plugin file, if it exists
        File cacheDir = new File(root, ".augmented");
        cacheDir.mkdirs();
        File augmentedFile = new File(cacheDir, pluginFile.getName());

        // Check if the augmented plugin needs to be (re)built
        long originalTimestamp = Math.max(pluginFile.lastModified(), sourceFile.lastModified());
        long augmentedTimestamp = (augmentedFile.isFile()) ? augmentedFile.lastModified() : 0;
        try {
            if(originalTimestamp > augmentedTimestamp) {
                mergeSource(pluginFile, sourceFile, augmentedFile);
            }
            return augmentedFile;
        } catch (Exception e) {
            if(reporter != null)
                reporter.error("Error merging plugin and source JARs");
            return null;
        }
    }

    private void mergeSource(File pluginFile, File sourceFile, File augmentedFile) throws Exception {
            Jar mainJar = new Jar(pluginFile);
            mainJar.setDoNotTouchManifest();
            Jar sourceJar = new Jar(sourceFile);

            mainJar.addAll(sourceJar, new Instruction(".*\\.java"), "OSGI-OPT/src");
            FileOutputStream out = new FileOutputStream(augmentedFile);
            mainJar.write(out);
    }

    public File get(String bsn, String range, Strategy strategy, Map<String, String> properties) throws Exception {
        File[] files = get(bsn, range);

        if (files == null || files.length == 0)
            return null;

        if (strategy == Strategy.LOWEST)
            return files[0];
        else
            return files[files.length - 1];
    }

    public File[] get(String bsn, String range) throws Exception {
        VersionRange r;
        if (range == null || range.equals("latest"))
            r = new VersionRange("0");
        else
            r = new VersionRange(range);
        Map<String, File> instances = index.get(bsn);
        if (instances == null) {
            return null;
        }

        List<File> result = new ArrayList<File>(instances.size());

        for (String version : instances.keySet()) {
            Version v = new Version(version);
            if (r.includes(v)) {
                File pluginFile = instances.get(version);
                File augmentedFile = getSourceAugmentedFile(pluginFile, bsn, version);

                if(augmentedFile != null && augmentedFile.isFile()) {
                    result.add(augmentedFile);
                } else if (pluginFile.isFile() || pluginFile.isDirectory()) {
                    result.add(pluginFile);
                }
            }
        }
        return result.toArray(new File[result.size()]);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }
    
    public String getLocation() {
    	return root.getAbsolutePath();
    }

}