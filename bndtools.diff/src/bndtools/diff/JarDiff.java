/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.diff;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import aQute.bnd.build.Project;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.VersionRange;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.Strategy;

public class JarDiff {

	public static final int PKG_SEVERITY_NONE = 0;
	public static final int PKG_SEVERITY_MICRO = 10; // Version missing on exported package, or bytecode modification
	public static final int PKG_SEVERITY_MINOR = 20; // Method or class added
	public static final int PKG_SEVERITY_MAJOR = 30; // Class deleted, method changed or deleted

	private static final String VERSION = "version";

	protected Map<String, PackageInfo> packages = new TreeMap<String, PackageInfo>();

	protected String bundleSymbolicName;
	
	private TreeSet<String> suggestedVersions;
	private String selectedVersion;
	private String currentVersion;

	private final Jar projectJar;
	private final Jar previousJar;
	
	private RepositoryPlugin baselineRepository;
	private RepositoryPlugin releaseRepository;

	public JarDiff(Jar projectJar, Jar previousJar) {
		suggestedVersions = new TreeSet<String>();
		this.projectJar = projectJar;
		this.previousJar = previousJar;
	}

	public void compare() throws Exception {

		Manifest projectManifest = projectJar.getManifest();
		Parameters projectExportedPackages = new Parameters(getAttribute(projectManifest, Constants.EXPORT_PACKAGE));
		Parameters projectImportedPackages = new Parameters(getAttribute(projectManifest, Constants.IMPORT_PACKAGE));

		bundleSymbolicName = stripInstructions(getAttribute(projectManifest, Constants.BUNDLE_SYMBOLICNAME));
		currentVersion = removeVersionQualifier(getAttribute(projectManifest, Constants.BUNDLE_VERSION)); // This is the version from the .bnd file

		Parameters previousExportedPackages;
		Parameters previousImportedPackages;
		Manifest previousManifest = null;
		if (previousJar != null) {
			previousManifest = previousJar.getManifest();
			previousExportedPackages = new Parameters(getAttribute(previousManifest, Constants.EXPORT_PACKAGE));
			previousImportedPackages = new Parameters(getAttribute(previousManifest, Constants.IMPORT_PACKAGE));

			// If no version in projectJar use previous version
			if (currentVersion == null) {
				currentVersion = removeVersionQualifier(getAttribute(previousManifest, Constants.BUNDLE_VERSION));
			}
		} else {
			previousExportedPackages = new Parameters(); // empty
			previousImportedPackages = new Parameters(); // empty
		}

		String prevName = stripInstructions(getAttribute(previousManifest, Constants.BUNDLE_SYMBOLICNAME));
		if (bundleSymbolicName != null && prevName != null && !bundleSymbolicName.equals(prevName)) {
			throw new IllegalArgumentException(Constants.BUNDLE_SYMBOLICNAME + " must be equal");
		}

		// Private packages
		if (previousJar != null) {
			for (String packageName : projectJar.getPackages()) {

				if (!projectExportedPackages.containsKey(packageName)) {
					PackageInfo pi = packages.get(packageName);
					if (pi == null) {
						pi = new PackageInfo(this, packageName);
					}
					Set<ClassInfo> projectClasses = getClassesFromPackage(pi, projectJar, packageName);
					if (projectClasses.size() == 0) {
						continue;
					}
					if (!packages.containsKey(packageName)) {
						packages.put(packageName, pi);
					}
					Set<ClassInfo> previousClasses = getClassesFromPackage(pi, previousJar, packageName);

					Set<ClassInfo> cis = pi.getClasses();

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
						cis.add(ci);
						if (prevCi != null && !Arrays.equals(ci.getSHA1(), prevCi.getSHA1())) {
							ci.setChangeCode(ClassInfo.CHANGE_CODE_MODIFIED);
							pi.setChangeCode(PackageInfo.CHANGE_CODE_MODIFIED);
							pi.setSeverity(PKG_SEVERITY_MICRO);
							continue;
						}
						if (prevCi == null) {
							ci.setChangeCode(ClassInfo.CHANGE_CODE_NEW);
							pi.setChangeCode(PackageInfo.CHANGE_CODE_MODIFIED);
							pi.setSeverity(PKG_SEVERITY_MICRO);
							continue;
						}
					}
					if (previousClasses != null) {
						for (ClassInfo prevCi : previousClasses) {
							if (!projectClasses.contains(prevCi)) {
								cis.add(prevCi);
								prevCi.setChangeCode(ClassInfo.CHANGE_CODE_REMOVED);
								pi.setChangeCode(PackageInfo.CHANGE_CODE_MODIFIED);
								pi.setSeverity(PKG_SEVERITY_MICRO);
							}
						}
					}
				}
			}
		}
		
		for (Entry<String, Attrs> entry : projectImportedPackages.entrySet()) {
			String packageName = entry.getKey();
			// New or modified packages
			PackageInfo pi = packages.get(packageName);
			if (pi == null) {
				pi = new PackageInfo(this, packageName);
				packages.put(packageName, pi);
			}
			pi.setImported(true);
			Map<String, String> packageMap = entry.getValue();
			String version = packageMap.get(VERSION);
			VersionRange projectVersion = null;
			if (version != null) {
				projectVersion = new VersionRange(version);
				pi.setSuggestedVersionRange(projectVersion.toString());
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
						pi.setVersionRange(previousVersion.toString());
						continue;
					}
					pi.setSeverity(PKG_SEVERITY_MINOR);
					pi.setChangeCode(PackageInfo.CHANGE_CODE_MODIFIED);
					pi.setVersionRange(previousVersion.toString());
					pi.setSuggestedVersionRange(projectVersion.toString());
					continue;
				}

				if (projectVersion != null) {
					pi.setSeverity(PKG_SEVERITY_MAJOR);
					pi.setChangeCode(PackageInfo.CHANGE_CODE_NEW);
					pi.setSuggestedVersionRange(projectVersion.toString());
					continue;
				}

				if (previousVersion != null) {
					pi.setSeverity(PKG_SEVERITY_MICRO);
					pi.setChangeCode(PackageInfo.CHANGE_CODE_MODIFIED);
					pi.setVersionRange(previousVersion.toString());
				}
			}
		}
		for (Entry<String, Attrs> entry : previousImportedPackages.entrySet()) {
			String packageName = entry.getKey();
			if (!projectImportedPackages.containsKey(packageName)) {
				// Removed Packages
				Map<String, String> prevPackageMap = entry.getValue();
				String previousVersion = prevPackageMap.get(VERSION);

				PackageInfo pi = packages.get(packageName);
				if (pi == null) {
					pi = new PackageInfo(this, packageName);
					packages.put(packageName, pi);
				}
				pi.setImported(true);
				pi.setSeverity(PKG_SEVERITY_MICRO);
				pi.setChangeCode(PackageInfo.CHANGE_CODE_REMOVED);
				pi.setVersionRange(previousVersion);
			}
		}

		for (Entry<String, Attrs> entry : projectExportedPackages.entrySet()) {
			String packageName = entry.getKey();
			// New or modified packages
			PackageInfo pi = packages.get(packageName);
			if (pi == null) {
				pi = new PackageInfo(this, packageName);
				packages.put(packageName, pi);
			}
			pi.setExported(true);

			Map<String, String> packageMap = entry.getValue();
			String packageVersion = removeVersionQualifier(packageMap.get(VERSION));

			/* can't be null */
			Set<ClassInfo> projectClasses = getClassesFromPackage(pi, projectJar, packageName);

			Set<ClassInfo> cis = pi.getClasses();

			String previousVersion = null;
			Set<ClassInfo> previousClasses = null;

			if (previousExportedPackages.containsKey(packageName)) {
				Map<String, String> prevPackageMap = previousExportedPackages.get(packageName);
				previousVersion = prevPackageMap.get(VERSION);
				previousClasses = getClassesFromPackage(pi, previousJar, packageName);
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
				int severity = getModificationSeverity(ci, prevCi);
				cis.add(ci);
				if (severity > PKG_SEVERITY_NONE) {
					// New or modified class
					if (severity > pi.getSeverity()) {
						pi.setSeverity(severity);
					}
				}
			}

			if (pi.getSeverity() > PKG_SEVERITY_NONE) {
				if (previousClasses == null) {
					// New package
					pi.setChangeCode(PackageInfo.CHANGE_CODE_NEW);
					pi.addSuggestedVersion(packageVersion);
				} else {
					// Modified package
					pi.setCurrentVersion(previousVersion);
					pi.setChangeCode(PackageInfo.CHANGE_CODE_MODIFIED);
				}
			}

			if (pi.getSeverity() == PKG_SEVERITY_NONE) {
				if (previousClasses != null && previousVersion == null) {
					// No change, but version missing on package
					pi.setSeverity(PKG_SEVERITY_MICRO);
					pi.setChangeCode(PackageInfo.CHANGE_CODE_VERSION_MISSING);
					pi.addSuggestedVersion(getCurrentVersion());
				}
			}

			if (previousClasses != null) {
				pi.setCurrentVersion(previousVersion);
				for (ClassInfo prevCi : previousClasses) {
					if (!projectClasses.contains(prevCi)) {
						int severity = getModificationSeverity(null, prevCi);
						cis.add(prevCi);
						if (severity > PKG_SEVERITY_NONE) {
							// Removed class
							if (severity > pi.getSeverity()) {
								pi.setSeverity(severity);
							}
							pi.setChangeCode(PackageInfo.CHANGE_CODE_MODIFIED);
						}
					}
				}
			}
		}

		for (Entry<String, Attrs> entry : previousExportedPackages.entrySet()) {
			String packageName = entry.getKey();
			if (!projectExportedPackages.containsKey(packageName)) {
				// Removed Packages
				Attrs prevPackageMap = entry.getValue();
				String previousVersion = prevPackageMap.get(VERSION);

				PackageInfo pi = packages.get(packageName);
				if (pi == null) {
					pi = new PackageInfo(this, packageName);
					packages.put(packageName, pi);
				}
				pi.setExported(true);
				pi.setChangeCode(PackageInfo.CHANGE_CODE_REMOVED);

				Set<ClassInfo> previousClasses = getClassesFromPackage(pi, previousJar, packageName);
				pi.setClasses(previousClasses);
				pi.setSeverity(PKG_SEVERITY_MAJOR);
				for (ClassInfo prevCi : previousClasses) {
					// Removed class
					getModificationSeverity(null, prevCi);
				}
				pi.setCurrentVersion(previousVersion);
			}
		}
	}

	private static int getModificationSeverity(ClassInfo clazz, ClassInfo previousClass) {

		int severity = PKG_SEVERITY_NONE;
		if (clazz != null) {
			for (MethodInfo method : clazz.getMethods()) {
				MethodInfo prevMethod = findMethod(previousClass, method);
				if (prevMethod == null) {
					severity =  PKG_SEVERITY_MINOR;
					method.setChangeCode(MethodInfo.CHANGE_NEW);
				}
			}
			for (FieldInfo field : clazz.getFields()) {
				FieldInfo prevField = findField(previousClass, field);
				if (prevField == null) {
					severity =  PKG_SEVERITY_MINOR;
					field.setChangeCode(FieldInfo.CHANGE_NEW);
				}
			}
		}

		if (previousClass != null) {
			for (MethodInfo prevMethod : previousClass.getMethods()) {
				MethodInfo method = findMethod(clazz, prevMethod);
				if (method == null) {
					severity = PKG_SEVERITY_MAJOR;
					prevMethod.setChangeCode(MethodInfo.CHANGE_REMOVED);
					if (clazz != null) {
						clazz.addPublicMethod(prevMethod);
					}
				}
			}
			for (FieldInfo prevField : previousClass.getFields()) {
				FieldInfo method = findField(clazz, prevField);
				if (method == null) {
					severity = PKG_SEVERITY_MAJOR;
					prevField.setChangeCode(FieldInfo.CHANGE_REMOVED);
					if (clazz != null) {
						clazz.addPublicField(prevField);
					}
				}
			}
		}

		if (clazz != null && previousClass != null) {
			if (severity > PKG_SEVERITY_NONE) {
				clazz.setChangeCode(ClassInfo.CHANGE_CODE_MODIFIED);
			} else {
				if (!Arrays.equals(clazz.getSHA1(), previousClass.getSHA1())) {
					clazz.setChangeCode(ClassInfo.CHANGE_CODE_MODIFIED);
					severity = PKG_SEVERITY_MICRO;
				} else {
					clazz.setChangeCode(ClassInfo.CHANGE_CODE_NONE);
				}
			}
		} else if (clazz != null && previousClass == null) {
			clazz.setChangeCode(ClassInfo.CHANGE_CODE_NEW);
			if (severity == PKG_SEVERITY_NONE) {
				severity = PKG_SEVERITY_MINOR;
			}
		} else if (clazz == null && previousClass != null) {
			previousClass.setChangeCode(ClassInfo.CHANGE_CODE_REMOVED);
			if (severity == PKG_SEVERITY_NONE) {
				severity = PKG_SEVERITY_MAJOR;
			}
		}

		return severity;
	}

	private static MethodInfo findMethod(ClassInfo info, MethodInfo methodToFind) {
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

	private static FieldInfo findField(ClassInfo info, FieldInfo fieldToFind) {
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
		String pn = packageName.replace('.', '/');
		Map<String, Map<String, Resource>> dirs = jar.getDirectories();
		if (dirs == null) {
			return Collections.emptySet();
		}
		Map<String, Resource> res = dirs.get(pn);
		if (res == null) {
			return Collections.emptySet();
		}
		Set<ClassInfo> ret = new TreeSet<ClassInfo>();

		for (Map.Entry<String, Resource> me : res.entrySet()) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if (me.getKey().endsWith(".class")) {
				InputStream is = null;
				try {
					is = me.getValue().openInputStream();
					byte[] bytes = new byte[8092];
					int bytesRead = 0;
					while ((bytesRead = is.read(bytes, 0, 8092)) != -1) {
						baos.write(bytes, 0, bytesRead);
					}

					byte[] classBytes = baos.toByteArray();
					MessageDigest md = MessageDigest.getInstance("SHA1");
					md.update(classBytes);
					byte[] digest = md.digest();
					ClassReader cr = new ClassReader(classBytes);
					ClassInfo ca = new ClassInfo(pi, digest);
					cr.accept(ca, 0);

					for (int i = 0; i < ca.methods.size(); i++) {
						MethodNode mn = (MethodNode) ca.methods.get(i);
						// Ignore anything but public and protected methods
						if ((mn.access & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC || (mn.access & Opcodes.ACC_PROTECTED) == Opcodes.ACC_PROTECTED) {
							MethodInfo mi = new MethodInfo(mn, ca);
							ca.addPublicMethod(mi);
						}
					}
					for (int i = 0; i < ca.fields.size(); i++) {
						FieldNode mn = (FieldNode) ca.fields.get(i);
						// Ignore anything but public fields
						if ((mn.access & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC || (mn.access & Opcodes.ACC_PROTECTED) == Opcodes.ACC_PROTECTED) {
							FieldInfo mi = new FieldInfo(mn, ca);
							ca.addPublicField(mi);
						}
					}
					ret.add(ca);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		return ret;
	}

	public Set<PackageInfo> getExportedPackages() {
		Set<PackageInfo> ret = new TreeSet<PackageInfo>();
		for (PackageInfo pi : packages.values()) {
			if (!pi.isExported()) {
				continue;
			}
			ret.add(pi);
		}
		return ret;
	}
	public Set<PackageInfo> getPrivatePackages() {
		Set<PackageInfo> ret = new TreeSet<PackageInfo>();
		for (PackageInfo pi : packages.values()) {
			if (pi.isExported() || pi.isImported()) {
				continue;
			}
			ret.add(pi);
		}
		return ret;
	}

	public Set<PackageInfo> getNewExportedPackages() {
		Set<PackageInfo> ret = new TreeSet<PackageInfo>();
		for (PackageInfo pi : getExportedPackages()) {
			if (pi.getChangeCode() != PackageInfo.CHANGE_CODE_NEW) {
				continue;
			}
			ret.add(pi);
		}
		return ret;
	}

	public Collection<PackageInfo> getModifiedExportedPackages() {
		Set<PackageInfo> ret = new TreeSet<PackageInfo>();
		for (PackageInfo pi : getExportedPackages()) {
			if (pi.getChangeCode() != PackageInfo.CHANGE_CODE_MODIFIED) {
				continue;
			}
			ret.add(pi);
		}
		return ret;
	}
	public Collection<PackageInfo> getRemovedExportedPackages() {
		Set<PackageInfo> ret = new TreeSet<PackageInfo>();
		for (PackageInfo pi : getExportedPackages()) {
			if (pi.getChangeCode() != PackageInfo.CHANGE_CODE_REMOVED) {
				continue;
			}
			ret.add(pi);
		}
		return ret;
	}
	public Set<PackageInfo> getChangedExportedPackages() {
		Set<PackageInfo> ret = new TreeSet<PackageInfo>();
		for (PackageInfo pi : getExportedPackages()) {
			if (pi.getChangeCode() == PackageInfo.CHANGE_CODE_NONE) {
				continue;
			}
			ret.add(pi);
		}
		return ret;
	}

	public Set<PackageInfo> getChangedPrivatePackages() {
		Set<PackageInfo> ret = new TreeSet<PackageInfo>();
		for (PackageInfo pi : getPrivatePackages()) {
			if (pi.getChangeCode() == PackageInfo.CHANGE_CODE_NONE) {
				continue;
			}
			ret.add(pi);
		}
		return ret;
	}

	public Collection<PackageInfo> getImportedPackages() {
		Set<PackageInfo> ret = new TreeSet<PackageInfo>();
		for (PackageInfo pi : packages.values()) {
			if (!pi.isImported()) {
				continue;
			}
			ret.add(pi);
		}
		return ret;
	}

	public Set<PackageInfo> getNewImportedPackages() {
		Set<PackageInfo> ret = new TreeSet<PackageInfo>();
		for (PackageInfo pi : getImportedPackages()) {
			if (pi.getChangeCode() != PackageInfo.CHANGE_CODE_NEW) {
				continue;
			}
			ret.add(pi);
		}
		return ret;
	}

	public Collection<PackageInfo> getModifiedImportedPackages() {
		Set<PackageInfo> ret = new TreeSet<PackageInfo>();
		for (PackageInfo pi : getImportedPackages()) {
			if (pi.getChangeCode() != PackageInfo.CHANGE_CODE_MODIFIED) {
				continue;
			}
			ret.add(pi);
		}
		return ret;
	}
	public Collection<PackageInfo> getRemovedImportedPackages() {
		Set<PackageInfo> ret = new TreeSet<PackageInfo>();
		for (PackageInfo pi : getImportedPackages()) {
			if (pi.getChangeCode() != PackageInfo.CHANGE_CODE_REMOVED) {
				continue;
			}
			ret.add(pi);
		}
		return ret;
	}
	public Set<PackageInfo> getChangedImportedPackages() {
		Set<PackageInfo> ret = new TreeSet<PackageInfo>();
		for (PackageInfo pi : getImportedPackages()) {
			if (pi.getChangeCode() == PackageInfo.CHANGE_CODE_NONE) {
				continue;
			}
			ret.add(pi);
		}
		return ret;
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
			try {
				if (baselineRepository != null) {
					File file =  baselineRepository.get(symbolicName, projectVersion.toString(), Strategy.EXACT, null);
					if (file != null) {
						currentJar = new Jar(file);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			JarDiff diff = new JarDiff(jar, currentJar);
			diff.setBaselineRepository(baselineRepository);
			diff.compare();
			diff.calculatePackageVersions();
			return diff;
		}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return null;
	}
	
	public void calculatePackageVersions() {

		int highestSeverity = PKG_SEVERITY_NONE;

		for (PackageInfo pi : getExportedPackages()) {
			if (pi.getChangeCode() != PackageInfo.CHANGE_CODE_MODIFIED) {
				continue;
			}
			String version = getVersionString(projectJar, pi.getPackageName());
			if (version == null) {
				version = pi.getCurrentVersion();
			}
			String mask;
			if (pi.getSeverity() > highestSeverity) {
				highestSeverity = pi.getSeverity();
			}
			switch(pi.getSeverity()) {
			case PKG_SEVERITY_MINOR :
				mask = "=+0";
				break;
			case PKG_SEVERITY_MAJOR :
				mask = "+00";
				break;
			default:
				mask = null;
			}
			if (mask != null) {
				String suggestedVersion = _version(new String[] { "", mask, version});
				pi.addSuggestedVersion(suggestedVersion);
			} else {
				pi.addSuggestedVersion(version);
			}
		}

		for (PackageInfo pi : getImportedPackages()) {
			if (pi.getChangeCode() != PackageInfo.CHANGE_CODE_REMOVED) {
				continue;
			}
			String mask;
			if (pi.getSeverity() > highestSeverity) {
				highestSeverity = pi.getSeverity();
			}
			switch(pi.getSeverity()) {
			case PKG_SEVERITY_MINOR :
				mask = "=+0";
				break;
			case PKG_SEVERITY_MAJOR :
				mask = "+00";
				break;
			default:
				mask = null;
			}
			if (mask != null) {
				String suggestedVersion = "[" + _version(new String[] { "", mask, currentVersion}) + "]";
				pi.addSuggestedVersion(suggestedVersion);
			} else {
				pi.addSuggestedVersion(currentVersion);
			}
		}

		String mask;
		switch(highestSeverity) {
		case PKG_SEVERITY_MINOR :
			mask = "=+0";
			break;
		case PKG_SEVERITY_MAJOR :
			mask = "+00";
			break;
		default:
			mask = "==+";
		}

		String bundleVersion = currentVersion == null ? "0.0.0" : currentVersion;
		String unqualifiedVersion = removeVersionQualifier(bundleVersion);

		String suggestedVersion = _version(new String[] { "", mask, unqualifiedVersion});
		suggestedVersions.add(suggestedVersion);
		if (suggestVersionOne(suggestedVersion)) {
			suggestedVersions.add("1.0.0");
		}

		for (PackageInfo pi : getExportedPackages()) {
			if (pi.getChangeCode() != PackageInfo.CHANGE_CODE_NEW) {
				continue;
			}
			// Obey packageinfo if it exist
			String version = getVersionString(projectJar, pi.getPackageName());
			if (version != null) {
				pi.addSuggestedVersion(version);
				if (suggestVersionOne(version)) {
					pi.addSuggestedVersion("1.0.0");
				}
			} else {
				if (pi.getSuggestedVersion() == null || pi.getSuggestedVersion().length() == 0 || "0.0.0".equals(pi.getSuggestedVersion())) {
					pi.addSuggestedVersion(suggestedVersion);
				}
				if (suggestVersionOne(suggestedVersion)) {
					pi.addSuggestedVersion("1.0.0");
				}
			}
			
		}
	}
	
	private static boolean suggestVersionOne(String version) {
		aQute.bnd.osgi.Version aQuteVersion = new aQute.bnd.osgi.Version(version);
		if (aQuteVersion.compareTo(new aQute.bnd.osgi.Version("1.0.0")) < 0) {
			return true;
		}
		return false;
	}

	// From aQute.libg.version.Macro _version. Without dependencies on project and properties
	private static String _version(String[] args) {
		String mask = args[1];

		aQute.bnd.osgi.Version version = new aQute.bnd.osgi.Version(args[2]);
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
					default:
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

	private static String getVersionString(Jar jar, String packageName) {
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
				try {is.close();} catch (Exception e) {
					/* ignore */
				}
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
	public static String getSeverityText(int severity) {
		switch (severity) {
		case PKG_SEVERITY_MINOR : {
			return "Minor (Method or class added)";
		}
		case PKG_SEVERITY_MAJOR : {
			return "Major (Class deleted, method changed or deleted)";
		}
		default: {
			return "";
		}
		}
	}

	
	
	public static void printDiff(JarDiff diff, PrintStream out) {
		out.println();
		out.println("============================================");
		out.println("Bundle " + diff.getSymbolicName() + ":");
		out.println("============================================");
		out.println("Version: " + diff.getCurrentVersion() + (diff.getSuggestedVersion() != null ? " -> Suggested Version: " + diff.getSuggestedVersion() : ""));
		if (diff.getModifiedExportedPackages().size() > 0) {
			out.println();
			out.println("Modified Exported Packages:");
			out.println("---------------------------");
			for (PackageInfo pi : diff.getModifiedExportedPackages()) {
				out.println(pi.getPackageName() + " " + pi.getCurrentVersion() + "   : " + getSeverityText(pi.getSeverity()) +  (pi.getSuggestedVersion() != null ? " -> Suggested version: " + pi.getSuggestedVersion() : ""));
				for (ClassInfo ci :pi.getChangedClasses()) {
					out.println("           " + ci.toString());
				}
			}
		}

		if (diff.getModifiedImportedPackages().size() > 0) {
			out.println();
			out.println("Modified Imported Packages:");
			out.println("---------------------------");
			for (PackageInfo pi : diff.getModifiedImportedPackages()) {
				out.println(pi.getPackageName() + " " + pi.getVersionRange() + "   : " + getSeverityText(pi.getSeverity()) +  (pi.getSuggestedVersionRange() != null ? " -> Suggested version range: " + pi.getSuggestedVersionRange() : ""));
				for (ClassInfo ci :pi.getChangedClasses()) {
					out.println("           " + ci.toString());
				}
			}
		}

		if (diff.getNewExportedPackages().size() > 0) {
			out.println();
			out.println("Added Exported Packages:");
			out.println("------------------------");
			for (PackageInfo pi : diff.getNewExportedPackages()) {
				out.println(pi.getPackageName() + (pi.getSuggestedVersion() != null ? " -> Suggested version: " + pi.getSuggestedVersion() : ""));
				for (ClassInfo ci :pi.getChangedClasses()) {
					out.println("           " + ci.toString());
				}
			}
		}

		if (diff.getNewImportedPackages().size() > 0) {
			out.println();
			out.println("Added Imported Packages:");
			out.println("------------------------");
			for (PackageInfo pi : diff.getNewImportedPackages()) {
				out.println(pi.getPackageName() + (pi.getSuggestedVersionRange() != null ? " -> Suggested version range: " + pi.getSuggestedVersionRange() : ""));
				for (ClassInfo ci :pi.getChangedClasses()) {
					out.println("           " + ci.toString());
				}
			}
		}

		if (diff.getRemovedExportedPackages().size() > 0) {
			out.println();
			out.println("Deleted Exported Packages:");
			out.println("--------------------------");
			for (PackageInfo pi : diff.getRemovedExportedPackages()) {
				out.println(pi.getPackageName() + " " + pi.getCurrentVersion());
			}
		}

		if (diff.getRemovedImportedPackages().size() > 0) {
			out.println();
			out.println("Deleted Imported Packages:");
			out.println("--------------------------");
			for (PackageInfo pi : diff.getRemovedImportedPackages()) {
				out.println(pi.getPackageName() + " " + pi.getVersionRange());
			}
		}

	}

	public String getSymbolicName() {
		return bundleSymbolicName;
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
	
	public static String getAttribute(Manifest manifest, String attributeName) {
		if (manifest != null && attributeName != null) {
			return (String) manifest.getMainAttributes().get(new Attributes.Name(attributeName));
		}
		return null;
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

	public String getCurrentVersion() {
		return currentVersion;
	}

	public String getSuggestedVersion() {
		if (suggestedVersions.size() > 0) {
			return suggestedVersions.last();
		}
		return null;
	}

	public TreeSet<String> getSuggestedVersions() {
		return suggestedVersions;
	}
	
	public String getSelectedVersion() {
		if (selectedVersion == null) {
			return getSuggestedVersion();
		}
		return selectedVersion;
	}
	
	public void setSelectedVersion(String selectedVersion) {
		this.selectedVersion = selectedVersion;
	}
	
	public RepositoryPlugin getBaselineRepository() {
		return baselineRepository;
	}

	public void setBaselineRepository(RepositoryPlugin baselineRepository) {
		this.baselineRepository = baselineRepository;
	}

	public RepositoryPlugin getReleaseRepository() {
		return releaseRepository;
	}

	public void setReleaseRepository(RepositoryPlugin releaseRepository) {
		this.releaseRepository = releaseRepository;
	}

}
