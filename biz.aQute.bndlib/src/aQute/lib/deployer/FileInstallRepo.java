package aQute.lib.deployer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.lib.osgi.*;
import aQute.libg.header.*;
import aQute.libg.reporter.*;
import aQute.libg.version.*;

public class FileInstallRepo extends FileRepo {

	String group;
	boolean dirty;
	Reporter reporter;
	Pattern              REPO_FILE   = Pattern
    .compile("([-a-zA-z0-9_\\.]+)-([0-9\\.]+)\\.(jar|lib)");
	
    public void setProperties(Map<String, String> map) {
    	super.setProperties(map);
    	group = map.get("group");
    }
    public void setReporter(Reporter reporter) {
    	super.setReporter(reporter);
        this.reporter = reporter;
    }

    public File put(Jar jar) throws Exception {
        dirty = true;
        Manifest manifest = jar.getManifest();
        if (manifest == null)
            throw new IllegalArgumentException("No manifest in JAR: " + jar);

        String bsn = manifest.getMainAttributes().getValue(
                Analyzer.BUNDLE_SYMBOLICNAME);
        if (bsn == null)
            throw new IllegalArgumentException("No Bundle SymbolicName set");

        Parameters b = Processor.parseHeader(bsn, null);
        if (b.size() != 1)
            throw new IllegalArgumentException("Multiple bsn's specified " + b);

        for (String key : b.keySet()) {
            bsn = key;
            if (!Verifier.SYMBOLICNAME.matcher(bsn).matches())
                throw new IllegalArgumentException(
                        "Bundle SymbolicName has wrong format: " + bsn);
        }

        String versionString = manifest.getMainAttributes().getValue(
                Analyzer.BUNDLE_VERSION);
        Version version;
        if (versionString == null)
            version = new Version();
        else
            version = new Version(versionString);

        File dir;
        if (group == null) {
        	dir = getRoot();
        } else {
        	dir= new File(getRoot(), group);
        	dir.mkdirs();
        }
        String fName = bsn + "-" + version.getMajor() + "."
                + version.getMinor() + "." + version.getMicro() + ".jar";
        File file = new File(dir, fName);

        jar.write(file);
        fireBundleAdded(jar, file);

        file = new File(dir, bsn + "-latest.jar");
        if (file.isFile() && file.lastModified() < jar.lastModified()) {
            jar.write(file);
        }
        return file;
    }
    public boolean refresh() {
        if ( dirty ) {
            dirty = false;
            return true;
        } else 
            return false;
    }
	@Override
	public List<String> list(String regex) {
	       Instruction pattern = null;
	        if (regex != null)
	            pattern = new Instruction(regex);

	        String list[] = getRoot().list();
	        List<String> result = new ArrayList<String>();
	        for (String f : list) {
                Matcher m = REPO_FILE.matcher(f);
                if (!m.matches()) {
                	continue;
                }
                String s = m.group(1);
	            if (pattern == null || pattern.matches(s))
	                result.add(s);
	        }
	        return result;
	}
	@Override
	public File[] get(String bsn, String versionRange) throws MalformedURLException {
	       // If the version is set to project, we assume it is not
        // for us. A project repo will then get it.
        if (versionRange != null && versionRange.equals("project"))
            return null;

        //
        // The version range we are looking for can
        // be null (for all) or a version range.
        //
        VersionRange range;
        if (versionRange == null || versionRange.equals("latest")) {
            range = new VersionRange("0");
        } else
            range = new VersionRange(versionRange);

        //
        // Iterator over all the versions for this BSN.
        // Create a sorted map over the version as key
        // and the file as URL as value. Only versions
        // that match the desired range are included in
        // this list.
        //
        File instances[] = getRoot().listFiles();
        SortedMap<Version, File> versions = new TreeMap<Version, File>();
        for (int i = 0; i < instances.length; i++) {
            Matcher m = REPO_FILE.matcher(instances[i].getName());
            if (m.matches() && m.group(1).equals(bsn)) {
                String versionString = m.group(2);
                Version version;
                if (versionString.equals("latest"))
                    version = new Version(Integer.MAX_VALUE);
                else
                    version = new Version(versionString);

                if (range.includes(version))
                    versions.put(version, instances[i]);
            }
        }
        return versions.values().toArray(new File[versions.size()]);
	}

}
