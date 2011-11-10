package aQute.lib.jardiff;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import org.osgi.framework.Constants;

import aQute.bnd.build.*;
import aQute.bnd.service.*;
import aQute.lib.jardiff.PackageDiff.PackageSeverity;
import aQute.lib.jardiff.java.*;
import aQute.lib.osgi.*;
import aQute.libg.version.*;

public class JarDiff implements Diff, VersionDiff {

	public static final Version VERSION_ONE = Version.parseVersion("1.0.0");
	
	protected String bundleSymbolicName;
	
	private TreeSet<Version> suggestedVersions;
	private Version newVersion;
	private Version oldVersion;

	private JavaDiff javaDiff;
	
	final Jar newJar;
	final Jar oldJar;
	
	final List<Diff> diffs = new ArrayList<Diff>();

	private RepositoryPlugin baselineRepository;
	
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
			//TODO: This should be a Diff...
			throw new IllegalArgumentException(Constants.BUNDLE_SYMBOLICNAME + " must be equal");
		}

		// Java 
		javaDiff = new JavaDiff(this);
		javaDiff.compare();
		
		diffs.add(javaDiff);

		for (DiffPlugin plugin : getDiffPlugins()) {
			
			PluginGroup grp = new PluginGroup(this, plugin);
			for (String path : newJar.getResources().keySet()) {
	            Resource ra = newJar.getResource(path);
	            Resource rb = null;
	            if (oldJar != null) {
	            	rb = oldJar.getResource(path);
	            }
	            
            	Collection<Diff> pluginDiffs = plugin.diff(grp, path, ra, rb);
            	grp.setContained(pluginDiffs);
			}
		}
	}

	static Collection<DiffPlugin> getDiffPlugins() {
		//TODO : Plugins
		//return Arrays.asList((DiffPlugin) new ManifestDiffPlugin());
		return Collections.emptyList();
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
		
		JavaDiff javaDiff = getJavaDiff();
		PackageSeverity severity = javaDiff.getHighestSeverity();
		
		Version oldVersion = getOldVersion();
		if (oldVersion == null) {
			oldVersion = Version.emptyVersion;
		}
		
		Version suggestedVersion;
		switch (severity) {
		case MAJOR :
			suggestedVersion = new Version(oldVersion.getMajor() + 1, 0, 0, oldVersion.getQualifier());
			break;
		case MINOR :
			suggestedVersion = new Version(oldVersion.getMajor(), oldVersion.getMinor() + 1, 0, oldVersion.getQualifier());
			break;
		default:
			suggestedVersion = new Version(oldVersion.getMajor(), oldVersion.getMinor(), oldVersion.getMicro() + 1, oldVersion.getQualifier());
		}
		
		addSuggestedVersion(suggestedVersion);
		if (suggestVersionOne(suggestedVersion)) {
			addSuggestedVersion(VERSION_ONE);
		}
	}

	private JavaDiff getJavaDiff() {
		for (Diff diff : diffs) {
			if (diff instanceof JavaDiff) {
				return (JavaDiff) diff;
			}
		}
		return null;
	}
	
	public static boolean suggestVersionOne(Version version) {
		if (version.compareTo(VERSION_ONE) < 0) {
			return true;
		}
		return false;
	}

	public static JarDiff createJarDiff(Project project, RepositoryPlugin baselineRepository, String bsn) {
		try {
		List<Builder> builders = project.getBuilder(null).getSubBuilders();
		Builder builder = null;
		for (Builder b : builders) {
			if (bsn.equals(b.getBsn())) {
				builder = b;
				break;
			}
		}
		if (builder != null) {
			Jar jar = builder.build();

			String bundleVersion = builder.getProperty(Constants.BUNDLE_VERSION);
			if (bundleVersion == null) {
				builder.setProperty(Constants.BUNDLE_VERSION, "0.0.0");
				bundleVersion = "0.0.0";
			}

			String unqualifiedVersion = removeVersionQualifier(bundleVersion);
			Version projectVersion = Version.parseVersion(unqualifiedVersion);

			String symbolicName = jar.getManifest().getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
			if (symbolicName == null) {
				symbolicName = jar.getName().substring(0, jar.getName().lastIndexOf('-'));
			}

			Jar currentJar = null;
			VersionRange range = new VersionRange("[" + projectVersion.toString() + "," + projectVersion.toString() + "]");
			try {
				if (baselineRepository != null) {
					File[] files =  baselineRepository.get(symbolicName, range.toString());
					if (files != null && files.length > 0) {
						currentJar = new Jar(files[0]);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			JarDiff diff = new JarDiff(jar, currentJar);
			diff.setBaselineRepository(baselineRepository);
			diff.compare();
			diff.calculateVersions();
			return diff;
		}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return null;
	}
	
	public void setBaselineRepository(RepositoryPlugin baselineRepository) {
		this.baselineRepository = baselineRepository;
	}

	public RepositoryPlugin getBaselineRepository() {
		return baselineRepository;
	}

	private static class PluginGroup implements Group {

		private Diff container;
		private String name;
		private Collection<? extends Diff> contained;
		
		public PluginGroup(Diff container, DiffPlugin diffplugin) {
			this.name = diffplugin.getName();
			this.container = container;
		}
		
		public Delta getDelta() {
			boolean added = false;
			boolean modified = false;
			boolean removed = false;
			for (Diff diff : getContained()) {
				switch (diff.getDelta()) {
				case ADDED :
					added = true;
					continue;
				case MODIFIED :
					modified = true;
					continue;
				case REMOVED :
					removed = true;
					continue;
				}
			}
			
			if (modified || added && removed) {
				return Delta.MODIFIED;
			}
			if (added) {
				return Delta.ADDED;
			}
			return Delta.REMOVED;
		}

		public String getName() {
			return name;
		}

		public Diff getContainer() {
			return container;
		}

		public Collection<? extends Diff> getContained() {
			return contained;
		}

		public String explain() {
			return getDelta() + " " + getName();
		}
	
		public void setContained(Collection<? extends Diff> contained) {
			this.contained = contained;
		}
	}
}
