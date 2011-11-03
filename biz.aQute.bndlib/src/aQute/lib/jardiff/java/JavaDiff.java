/*******************************************************************************
 * Copyright (c) 2011 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package aQute.lib.jardiff.java;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.*;

import org.osgi.framework.Constants;

import aQute.bnd.build.*;
import aQute.bnd.service.*;
import aQute.lib.jardiff.*;
import aQute.lib.jardiff.PackageDiff.PackageSeverity;
import aQute.lib.osgi.*;
import aQute.libg.header.*;
import aQute.libg.version.*;

public class JavaDiff implements Group {

	private static final String VERSION = "version";
	
	protected final Map<String, PackageInfo> packages = new TreeMap<String, PackageInfo>();

	protected final JarDiff jarDiff;
	
	public JavaDiff(JarDiff jarDiff) {
		this.jarDiff = jarDiff; 
	}
	
	public void compare() throws Exception {

		Manifest projectManifest = jarDiff.getNewJar().getManifest();
		Map<String, Map<String, String>> projectExportedPackages = OSGiHeader.parseHeader(BundleJarDiff.getAttribute(projectManifest, Constants.EXPORT_PACKAGE), null);
		Map<String, Map<String, String>> projectImportedPackages = OSGiHeader.parseHeader(BundleJarDiff.getAttribute(projectManifest, Constants.IMPORT_PACKAGE), null);

		Map<String, Map<String, String>> previousPackages;
		Map<String, Map<String, String>> previousImportedPackages;
		Manifest previousManifest = null;
		if (jarDiff.getOldJar() != null) {
			previousManifest = jarDiff.getOldJar().getManifest();
			previousPackages = OSGiHeader.parseHeader(BundleJarDiff.getAttribute(previousManifest, Constants.EXPORT_PACKAGE), null);
			previousImportedPackages = OSGiHeader.parseHeader(BundleJarDiff.getAttribute(previousManifest, Constants.IMPORT_PACKAGE), null);
		} else {
			previousPackages = Collections.emptyMap();
			previousImportedPackages = Collections.emptyMap();
		}

		for (String packageName : projectImportedPackages.keySet()) {
			// New or modified packages
			PackageInfo pi = packages.get(packageName);
			if (pi == null) {
				pi = new PackageInfo(this, packageName);
				packages.put(packageName, pi);
			}
			pi.setImported(true);
			
			Map<String, String> packageMap = projectImportedPackages.get(packageName);
			String version = packageMap.get(VERSION);
			VersionRange projectVersion = null;
			if (version != null) {
				projectVersion = new VersionRange(version);
				pi.setSuggestedVersionRange(projectVersion);
			}
			if (previousImportedPackages.containsKey(packageName)) {
				Map<String, String> prevPackageMap = previousImportedPackages.get(packageName);
				version = prevPackageMap.get(VERSION);
				VersionRange previousVersion = null;
				if (version != null) {
					previousVersion = new VersionRange(version);
				}
				// No change, no versions
				if (projectVersion == null && previousVersion == null) {
					continue;
				}
				// No change
				if (projectVersion != null && previousVersion != null) {
					if (projectVersion.getHigh().equals(previousVersion.getHigh()) &&
							projectVersion.getLow().equals(previousVersion.getLow())) {
						pi.setOldVersionRange(previousVersion);
						continue;
					}
					
					pi.setSeverity(PackageSeverity.MINOR);
					pi.setDelta(Delta.MODIFIED);
					pi.setOldVersionRange(previousVersion);
					pi.setSuggestedVersionRange(projectVersion);
					continue;
				}

				if (projectVersion != null) {
					pi.setSeverity(PackageSeverity.MAJOR);
					pi.setDelta(Delta.ADDED);
					pi.setSuggestedVersionRange(projectVersion);
					continue;
				}

				if (previousVersion != null) {
					pi.setSeverity(PackageSeverity.MICRO);
					pi.setDelta(Delta.MODIFIED);
					pi.setOldVersionRange(previousVersion);
				}
			}
		}
		for (String packageName : previousImportedPackages.keySet()) {
			if (!projectImportedPackages.containsKey(packageName)) {
				// Removed Packages
				Map<String, String> prevPackageMap = previousImportedPackages.get(packageName);
				String pv = prevPackageMap.get(VERSION);
				VersionRange previousVersion = null;
				if (pv != null) {
					previousVersion = new VersionRange(prevPackageMap.get(VERSION));
				}

				PackageInfo pi = packages.get(packageName);
				if (pi == null) {
					pi = new PackageInfo(this, packageName);
					packages.put(packageName, pi);
				}
				pi.setImported(true);
				pi.setSeverity(PackageSeverity.MICRO);
				pi.setDelta(Delta.REMOVED);
				pi.setOldVersionRange(previousVersion);
			}
		}

		for (String packageName : projectExportedPackages.keySet()) {
			// New or modified packages
			PackageInfo pi = packages.get(packageName);
			if (pi == null) {
				pi = new PackageInfo(this, packageName);
				packages.put(packageName, pi);
			}
			pi.setExported(true);

			Map<String, String> packageMap = projectExportedPackages.get(packageName);
			String ver = JarDiff.removeVersionQualifier(packageMap.get(VERSION));
			Version packageVersion = null;
			if (ver != null) {
				packageVersion = Version.parseVersion(ver);
			} else {
				packageVersion = new HighestVersion();
			}

			Set<ClassInfo> projectClasses = getClassesFromPackage(pi, jarDiff.getNewJar(), packageName);

			Set<ClassInfo> cis = pi.getClasses();

			Version previousVersion = null;
			Set<ClassInfo> previousClasses = null;
			if (previousPackages.containsKey(packageName)) {
				Map<String, String> prevPackageMap = previousPackages.get(packageName);
				previousVersion = Version.parseVersion(prevPackageMap.get(VERSION));
				previousClasses = getClassesFromPackage(pi, jarDiff.getOldJar(), packageName);
			}

			for (ClassInfo ci : projectClasses) {
				ClassInfo prevCi = null;
				if (previousClasses != null) {
					for (ClassInfo c : previousClasses) {
						if (c.equals(ci)) {
							prevCi = c;
							break;
						}
					}
				}
				PackageSeverity severity = getModificationSeverity(ci, prevCi);
				cis.add(ci);
				if (severity.value() > PackageSeverity.NONE.value()) {
					// New or modified class
					if (severity.value() > pi.getSeverity().value()) {
						pi.setSeverity(severity);
					}
				}
			}

			if (pi.getSeverity().value() > PackageSeverity.NONE.value()) {
				if (previousClasses == null) {
					// New package
					pi.setDelta(Delta.ADDED);
					pi.addSuggestedVersion(packageVersion);
				} else {
					// Modified package
					pi.setOldVersion(previousVersion);
					pi.setDelta(Delta.MODIFIED);
				}
			}

			if (pi.getSeverity() == PackageSeverity.NONE) {
				if (previousClasses != null && previousVersion == null) {
					// No change, but version missing on package
					pi.setSeverity(PackageSeverity.MICRO);
//					pi.setChangeCode(PackageInfo.CHANGE_CODE_VERSION_MISSING);
					pi.setDelta(Delta.MODIFIED);
					pi.addSuggestedVersion(jarDiff.getOldVersion());
				}
			}

			if (previousClasses != null) {
				pi.setOldVersion(previousVersion);
				for (ClassInfo prevCi : previousClasses) {
					if (projectClasses != null && !projectClasses.contains(prevCi)) {
						PackageSeverity severity = getModificationSeverity(null, prevCi);
						cis.add(prevCi);
						if (severity.value() > PackageSeverity.NONE.value()) {
							// Removed class
							if (severity.value() > pi.getSeverity().value()) {
								pi.setSeverity(severity);
							}
							pi.setDelta(Delta.MODIFIED);
						}
					}
				}
			}
		}

		for (String packageName : previousPackages.keySet()) {
			if (!projectExportedPackages.containsKey(packageName)) {
				// Removed Packages
				Map<String, String> prevPackageMap = previousPackages.get(packageName);
				Version previousVersion = Version.parseVersion(prevPackageMap.get(VERSION));

				PackageInfo pi = packages.get(packageName);
				if (pi == null) {
					pi = new PackageInfo(this, packageName);
					packages.put(packageName, pi);
				}
				pi.setExported(true);
				pi.setDelta(Delta.REMOVED);

				Set<ClassInfo> previousClasses = getClassesFromPackage(pi, jarDiff.getOldJar(), packageName);
				pi.setClasses(previousClasses);
				pi.setSeverity(PackageSeverity.MAJOR);
				for (ClassInfo prevCi : previousClasses) {
					// Removed class
					getModificationSeverity(null, prevCi);
				}
				pi.setOldVersion(previousVersion);
			}
		}
	}

	private PackageSeverity getModificationSeverity(ClassInfo classInfo, ClassInfo previousClass) {

		PackageSeverity severity = PackageSeverity.NONE;
		if (classInfo != null) {
			for (MethodInfo method : classInfo.getMethods()) {
				MethodInfo prevMethod = findMethod(previousClass, method);
				if (prevMethod == null) {
					severity =  PackageSeverity.MINOR;
					method.setDelta(Delta.ADDED);
				}
			}
			for (FieldInfo field : classInfo.getFields()) {
				FieldInfo prevField = findField(previousClass, field);
				if (prevField == null) {
					severity =  PackageSeverity.MINOR;
					field.setDelta(Delta.ADDED);
				}
			}
		}

		if (previousClass != null) {
			for (MethodInfo prevMethod : previousClass.getMethods()) {
				MethodInfo method = findMethod(classInfo, prevMethod);
				if (method == null) {
					severity = PackageSeverity.MAJOR;
					prevMethod.setDelta(Delta.REMOVED);
					if (classInfo != null) {
						classInfo.addMethod(prevMethod);
					}
				}
			}
			for (FieldInfo prevField : previousClass.getFields()) {
				FieldInfo method = findField(classInfo, prevField);
				if (method == null) {
					severity = PackageSeverity.MAJOR;
					prevField.setDelta(Delta.REMOVED);
					if (classInfo != null) {
						classInfo.addField(prevField);
					}
				}
			}
		}

		if (classInfo != null && previousClass != null) {
			if (severity.value() > PackageSeverity.NONE.value()) {
				classInfo.setDelta(Delta.MODIFIED);
			} else {
				classInfo.setDelta(Delta.UNCHANGED);
			}
		} else if (previousClass == null) {
			classInfo.setDelta(Delta.ADDED);
			if (severity == PackageSeverity.NONE) {
				severity = PackageSeverity.MINOR;
			}
		} else if (classInfo == null) {
			previousClass.setDelta(Delta.REMOVED);
			if (severity == PackageSeverity.NONE) {
				severity = PackageSeverity.MAJOR;
			}
		}

		return severity;
	}

	private MethodInfo findMethod(ClassInfo info, MethodInfo methodToFind) {
		if (info == null) {
			return null;
		}
		for (MethodInfo method : info.getMethods()) {
			if (!method.getName().equals(methodToFind.getName())) {
				continue;
			}
			if (method.getDesc() != null && !method.getDesc().equals(methodToFind.getDesc())) {
				continue;
			}
			return method;
		}
		return null;
	}

	private FieldInfo findField(ClassInfo info, FieldInfo fieldToFind) {
		if (info == null) {
			return null;
		}
		for (FieldInfo field : info.getFields()) {
			if (!field.getName().equals(fieldToFind.getName())) {
				continue;
			}
			if (field.getDesc() != null && !field.getDesc().equals(fieldToFind.getDesc())) {
				continue;
			}
			return field;
		}
		return null;
	}

	private static Set<ClassInfo> getClassesFromPackage(PackageInfo pi, Jar jar,
			String packageName) {
		packageName = packageName.replace('.', '/');
		Map<String, Map<String, Resource>> dirs = jar.getDirectories();
		if (dirs == null) {
			return Collections.emptySet();
		}
		Map<String, Resource> res = dirs.get(packageName);
		if (res == null) {
			return Collections.emptySet();
		}
		
		Set<ClassInfo> ret = new TreeSet<ClassInfo>();

		for (Map.Entry<String, Resource> me : res.entrySet()) {
			if (me.getKey().endsWith(".class")) {
				
				try {
										
					ClassInfo ca = new ClassInfo(pi, me.getKey(), me.getValue());
					ret.add(ca);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		return ret;
	}

	static Collection<Method> getMethods(Class<?> theClass) {
		
		// Get all public methods
		Set<Method> methods = new HashSet<Method>();
		for (Method method : theClass.getMethods()) {
			methods.add(method);
		}
		
		// Get protected methods
		Class<?> clazz = theClass;
		do {
			for (Method method : clazz.getDeclaredMethods()) {
				if ((method.getModifiers() & Modifier.PROTECTED) == Modifier.PROTECTED) {
					methods.add(method);
				}
			}
		} while ((clazz = clazz.getSuperclass()) != null);
		
		return methods;
	}
	
	static Collection<Field> getFields(Class<?> theClass) {
		
		// Get all public fields
		Set<Field> fields = new HashSet<Field>();
		for (Field field : theClass.getFields()) {
			fields.add(field);
		}
		
		// Get protected fields
		Class<?> clazz = theClass;
		do {
			for (Field field : clazz.getDeclaredFields()) {
				if ((field.getModifiers() & Modifier.PROTECTED) == Modifier.PROTECTED) {
					fields.add(field);
				}
			}
		} while ((clazz = clazz.getSuperclass()) != null);
		
		return fields;
	}
	
	public Collection<PackageInfo> getExportedPackages() {
		Set<PackageInfo> ret = new TreeSet<PackageInfo>();
		for (PackageInfo pi : packages.values()) {
			if (!pi.isExported()) {
				continue;
			}
			ret.add(pi);
		}
		return ret;
	}

	private Collection<PackageInfo> getExportedPackages(Delta delta, boolean exclude) {
		return getPackages(getExportedPackages(), delta, exclude);
	}
	
	private Collection<PackageInfo> getImportedPackages(Delta delta, boolean exclude) {
		return getPackages(getImportedPackages(), delta, exclude);
	}

	public Collection<PackageInfo> getPackages(Collection<PackageInfo> packages, Delta delta, boolean exclude) {
		Set<PackageInfo> packageInfos = new TreeSet<PackageInfo>();
		for (PackageInfo packageInfo : packages) {
			if (!exclude && packageInfo.getDelta() != delta || exclude && packageInfo.getDelta() == delta) {
				continue;
			}
			packageInfos.add(packageInfo);
		}
		return packageInfos;
	}

	public Collection<PackageInfo> getPackages() {
		return packages.values();
	}
	
	public Collection<PackageInfo> getNewExportedPackages() {
		return getExportedPackages(Delta.ADDED, false);
	}

	public Collection<PackageInfo> getModifiedExportedPackages() {
		return getExportedPackages(Delta.MODIFIED, false);
	}

	public Collection<PackageInfo> getRemovedExportedPackages() {
		return getExportedPackages(Delta.REMOVED, false);
	}

	public Collection<PackageInfo> getChangedExportedPackages() {
		return getExportedPackages(Delta.UNCHANGED, true);
	}

	public Collection<PackageInfo> getImportedPackages() {
		Set<PackageInfo> packageInfos = new TreeSet<PackageInfo>();
		for (PackageInfo packageInfo : packages.values()) {
			if (!packageInfo.isImported()) {
				continue;
			}
			packageInfos.add(packageInfo);
		}
		return packageInfos;
	}

	public Collection<PackageInfo> getNewImportedPackages() {
		return getImportedPackages(Delta.ADDED, false);
	}

	public Collection<PackageInfo> getModifiedImportedPackages() {
		return getImportedPackages(Delta.MODIFIED, false);
	}
	
	public Collection<PackageInfo> getRemovedImportedPackages() {
		return getImportedPackages(Delta.REMOVED, false);
	}

	public Collection<PackageInfo> getChangedImportedPackages() {
		return getImportedPackages(Delta.UNCHANGED, true);
	}

	public static List<BundleJarDiff> createJarDiffs(Project project, List<RepositoryPlugin> repos, List<File> subBundles) {

		List<BundleJarDiff> diffs = new ArrayList<BundleJarDiff>();

		try {
			project.refresh();
			List<Builder> builders = project.getBuilder(null).getSubBuilders();
			for (Builder b : builders) {

				if (subBundles != null) {
					if (!subBundles.contains(b.getPropertiesFile())) {
						continue;
					}
				}
				
				Jar jar = b.build();

				//List<String> errors = b.getErrors();

				String bundleVersion = b.getProperty(Constants.BUNDLE_VERSION);
				if (bundleVersion == null) {
				    b.setProperty(Constants.BUNDLE_VERSION, "0.0.0");
					bundleVersion = "0.0.0";
				}

				String unqualifiedVersion = BundleJarDiff.removeVersionQualifier(bundleVersion);
				Version projectVersion = Version.parseVersion(unqualifiedVersion);

				String symbolicName = jar.getManifest().getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
				if (symbolicName == null) {
					symbolicName = jar.getName().substring(0, jar.getName().lastIndexOf('-'));
				}
				File[] bundleJars = new File[0];
				for (RepositoryPlugin repo : repos) {
					try {
						File[] files =  repo.get(symbolicName, null);
						if (files != null && files.length > 0) {
							File[] tmp = new File[bundleJars.length + files.length];
							System.arraycopy(bundleJars, 0, tmp, 0, bundleJars.length);
							System.arraycopy(files, 0, tmp, bundleJars.length, files.length);
							bundleJars = tmp;
						}
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
				}

				Jar currentJar = null;
				for (RepositoryPlugin repo : repos) {
					VersionRange range = new VersionRange("[" + projectVersion.toString() + "," + projectVersion.toString() + "]");
					try {
						File[] files =  repo.get(symbolicName, range.toString());
						if (files != null && files.length > 0) {
							currentJar = new Jar(files[0]);
							break;
						}
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
				}

				BundleJarDiff diff = new BundleJarDiff(jar, currentJar);
				diff.compare();
				diff.calculateVersions();

				diffs.add(diff);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}


		return diffs;
	}

	public Delta getDelta() {
		return (getChangedExportedPackages().size() > 0 || getChangedExportedPackages().size() > 0 ? Delta.MODIFIED : jarDiff.getOldJar() == null ? Delta.ADDED : Delta.UNCHANGED);
	}

	public String getName() {
		return "Java Packages";
	}

	public Diff getContainer() {
		return jarDiff;
	}

	public Collection<? extends Diff> getContained() {
		return packages.values();
	}

	public String explain() {
		return String.valueOf(getDelta());
	}

	public String toString() {
		return getName() + " " + jarDiff.getSymbolicName();
	}
	
	
	// From aQute.libg.version.Macro _version. Without dependencies on project and properties
	public static  String _version(String[] args) {
		String mask = args[1];

		aQute.libg.version.Version version = new aQute.libg.version.Version(args[2]);
		StringBuilder sb = new StringBuilder();
		String del = "";

		for (int i = 0; i < mask.length(); i++) {
			char c = mask.charAt(i);
			String result = null;
			if (c != '~') {
				if (i == 3) {
					result = version.getQualifier();
				} else if (Character.isDigit(c)) {
					// Handle masks like +00, =+0
					result = String.valueOf(c);
				} else {
					int x = version.get(i);
					switch (c) {
					case '+':
						x++;
						break;
					case '-':
						x--;
						break;
					case '=':
						break;
					}
					result = Integer.toString(x);
				}
				if (result != null) {
					sb.append(del);
					del = ".";
					sb.append(result);
				}
			}
		}
		return sb.toString();

	}
	
	
	static String getVersionString(Jar jar, String packageName) {
		Resource resource = jar.getResource(getResourcePath(packageName, "packageinfo"));
		if (resource == null) {
			return null;
		}
		Properties packageInfo = new Properties();
		InputStream is = null;
		try {
			is = resource.openInputStream();
			packageInfo.load(is);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (is != null) {
				try {is.close();} catch (Exception e) {}
			}
		}
		String version = packageInfo.getProperty(VERSION);
		return version;
	}

	private static String getResourcePath(String packageName, String resourceName) {
		String s = packageName.replace('.', '/');
		s += "/" + resourceName;
		return s;
	}

	public static String getSeverityText(PackageSeverity severity) {
		switch (severity) {
		case MINOR : {
			return "Minor (Method or class added)";
		}
		case MAJOR : {
			return "Major (Class deleted, method changed or deleted)";
		}
		default: {
			return "";
		}
		}
	}
	
	class HighestVersion extends Version {

		private Version calculateVersion() {
			Version highest = Version.LOWEST;
			for (PackageInfo pi : getExportedPackages()) {
				if (pi.getSuggestedVersion() != null) {
					if (highest.compareTo(pi.getSuggestedVersion()) < 0) {
						highest = pi.getSuggestedVersion();
						continue;
					}
				}
				if (pi.getOldVersion() != null) {
					if (highest.compareTo(pi.getOldVersion()) < 0) {
						highest = pi.getOldVersion();
					}
				}
			}
			return highest;
		}
		
		@Override
		public int getMajor() {
			Version v = calculateVersion();
			return v.getMajor();
		}

		@Override
		public int getMinor() {
			Version v = calculateVersion();
			return v.getMinor();
		}

		@Override
		public int getMicro() {
			Version v = calculateVersion();
			return v.getMicro();
		}

		@Override
		public String getQualifier() {
			Version v = calculateVersion();
			return v.getQualifier();
		}

		@Override
		public int compareTo(Version other) {
	        if (other == this)
	            return 0;

	        if (!(other instanceof Version))
	            throw new IllegalArgumentException(
	                    "Can only compare versions to versions");

	        Version o = (Version) other;
	        if (getMajor() != o.getMajor())
	            return getMajor() - o.getMajor();

	        if (getMinor() != o.getMinor())
	            return getMinor() - o.getMinor();

	        if (getMicro() != o.getMicro())
	            return getMicro() - o.getMicro();

	        int c = 0;
	        if (getQualifier() != null)
	            c = 1;
	        if (o.getQualifier() != null)
	            c += 2;

	        switch (c) {
	        case 0:
	            return 0;
	        case 1:
	            return 1;
	        case 2:
	            return -1;
	        }
	        return getQualifier().compareTo(o.getQualifier());
		}

		@Override
		public String toString() {
			Version v = calculateVersion();
			return v.toString();
		}

		@Override
		public boolean equals(Object ot) {
			Version v = calculateVersion();
			return v.equals(ot);
		}

		@Override
		public int hashCode() {
			Version v = calculateVersion();
			return v.hashCode();
		}

		@Override
		public int get(int i) {
			Version v = calculateVersion();
			return v.get(i);
		}
	}

	public PackageSeverity getHighestSeverity() {
		PackageSeverity highestSeverity = PackageSeverity.NONE;

		for (PackageInfo pi : getPackages()) {
			if (pi.getSeverity().value() > highestSeverity.value()) {
				highestSeverity = pi.getSeverity();
			}
		}
		return highestSeverity;
	}

	public String getGroupName() {
		return "Java Packages";
	}

}
