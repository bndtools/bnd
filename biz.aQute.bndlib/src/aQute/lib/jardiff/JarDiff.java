package aQute.lib.jardiff;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import org.osgi.framework.Constants;

import aQute.lib.jardiff.java.*;
import aQute.lib.jardiff.manifest.*;
import aQute.lib.osgi.*;
import aQute.libg.version.Version;

public class JarDiff implements Diff, VersionDiff {

	public static final Version VERSION_ONE = Version.parseVersion("1.0.0");
	
	public static final String MANIFEST_MF = "META-INF/MANIFEST.MF";
	
	protected String bundleSymbolicName;
	
	private TreeSet<Version> suggestedVersions;
	private Version newVersion;
	private Version oldVersion;

	private JavaDiff javaDiff;
	
	final Jar newJar;
	final Jar oldJar;
	
	final List<Diff> diffs = new ArrayList<Diff>();
	
	public JarDiff(Jar newJar, Jar oldJar) {
		suggestedVersions = new TreeSet<Version>();
		this.newJar = newJar;
		this.oldJar = oldJar;
	}

	public String getSymbolicName() {
		return bundleSymbolicName;
	}

	public Jar getNewJar() {
		return newJar;
	}
	
	public Jar getOldJar() {
		return oldJar;
	}
	public void compare() throws Exception {

		Manifest projectManifest = newJar.getManifest();

		bundleSymbolicName = stripInstructions(getAttribute(projectManifest, Constants.BUNDLE_SYMBOLICNAME));
		oldVersion = Version.parseVersion(removeVersionQualifier(getAttribute(projectManifest, Constants.BUNDLE_VERSION))); // This is the version from the .bnd file

		Manifest previousManifest = null;
		if (oldJar != null) {
			previousManifest = oldJar.getManifest();

			// If no version in projectJar use previous version
			if (oldVersion == null) {
				oldVersion = Version.parseVersion(removeVersionQualifier(getAttribute(previousManifest, Constants.BUNDLE_VERSION)));
			}
		}

		String prevName = stripInstructions(getAttribute(previousManifest, Constants.BUNDLE_SYMBOLICNAME));
		if (bundleSymbolicName != null && prevName != null && !bundleSymbolicName.equals(prevName)) {
			throw new IllegalArgumentException(Constants.BUNDLE_SYMBOLICNAME + " must be equal");
		}

		// Java 
		javaDiff = new JavaDiff(this);
		javaDiff.compare();
		
		diffs.add(javaDiff);
		
		//TODO: Plugins
		
		// Manifest
		ManifestDiffPlugin plugin = new ManifestDiffPlugin();
		Resource newResource = newJar.getResource(MANIFEST_MF);
		Resource oldResource = oldJar.getResource(MANIFEST_MF);
		diffs.addAll(plugin.diff(this, MANIFEST_MF, newResource, oldResource));
		
		
	}

	public String explain() {
		CharArrayWriter writer = new CharArrayWriter();
		printDiff(this, new PrintWriter(writer));
		return new String(writer.toCharArray());
	}
	
	public Version getSuggestedVersion() {
		if (suggestedVersions.size() > 0) {
			return suggestedVersions.last();
		}
		return null;
	}

	public void addSuggestedVersion(Version version) {
		if (!suggestedVersions.contains(version)) {
			suggestedVersions.add(version);
		}
	}
	
	public Set<Version> getSuggestedVersions() {
		return suggestedVersions;
	}

	public Delta getDelta() {
		return oldJar == null ? Delta.ADDED : (javaDiff.getPackages(javaDiff.getPackages(), Delta.UNCHANGED, true).size() > 0) ? Delta.MODIFIED : Delta.UNCHANGED;
	}

	public String getName() {
		return getSymbolicName();
	}

	public Diff getContainer() {
		return null;
	}

	public Version getOldVersion() {
		return oldVersion;
	}

	public void setNewVersion(Version version) {
		this.newVersion = version;
	}

	public Version getNewVersion() {
		if (newVersion == null) {
			return getSuggestedVersion();
		}
		return newVersion;
	}

	public static JarDiff diff(Jar newJar, Jar oldJar) throws Exception {
		JarDiff jarDiff = new JarDiff(newJar, oldJar);
		jarDiff.compare();
		return jarDiff;
	}
	
	public Collection<? extends Diff> getContained() {
		return diffs;
	}

	public static void printDiff(JarDiff jarDiff, PrintWriter out) {
		out.println();
		out.println("Bundle " + jarDiff.getSymbolicName() + " : " + jarDiff.getOldVersion() + (jarDiff.getSuggestedVersion() != null ? " -> Suggested Version: " + jarDiff.getSuggestedVersion() : ""));
		out.println("============================================");
		out.println();

		for (Diff diff : jarDiff.getContained()) {
			explain(diff, out, 0);
		}
		out.flush();
	}

	private static void explain(Diff diff, PrintWriter out, int level) {
		indent(level, out);
		out.println(diff.explain());
		level++;
		for (Diff subDiff : diff.getContained()) {
			explain(subDiff, out, level);
		}
	}

	private static void indent(int level, PrintWriter out) {
		for(int i = 0; i < level; i++) {
			out.append("   ");
		}
	}

	public static String getAttribute(Manifest manifest, String attributeName) {
		if (manifest != null && attributeName != null) {
			return (String) manifest.getMainAttributes().get(new Attributes.Name(attributeName));
		}
		return null;
	}

	public static String stripInstructions(String header) {
		if (header == null) {
			return null;
		}
		int idx = header.indexOf(';');
		if (idx > -1) {
			return header.substring(0, idx);
		}
		return header;
	}

	public static String removeVersionQualifier(String version) {
		if (version == null) {
			return null;
		}
		// Remove qualifier
		String[] parts = version.split("\\.");
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (int i = 0; i < parts.length; i++) {
			if (i == 3) {
				break;
			}
			sb.append(sep);
			sb.append(parts[i]);
			sep = ".";
		}
		return sb.toString();
	}

	
	public void calculateVersions(Diff diff) {
		if (diff instanceof VersionDiff) {
			((VersionDiff)diff).calculateVersions();
		}
		for (Diff subDiff : diff.getContained()) {
			calculateVersions(subDiff);
		}
	}
	
	public void calculateVersions() {
		for (Diff diff : diffs) {
			calculateVersions(diff);
		}
	}

	public static boolean suggestVersionOne(Version version) {
		if (version.compareTo(new aQute.libg.version.Version("1.0.0")) < 0) {
			return true;
		}
		return false;
	}

}
