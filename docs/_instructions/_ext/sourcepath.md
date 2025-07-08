---
layout: default
class: Builder
title: -sourcepath 
summary: List of directory names that used to find sources. 
---


	public Collection<File> getSourcePath() {
		if (firstUse) {
			firstUse = false;
			String sp = getProperty(SOURCEPATH);
			if (sp != null) {
				Parameters map = parseHeader(sp);
				for (Iterator<String> i = map.keySet().iterator(); i.hasNext();) {
					String file = i.next();
					if (!isDuplicate(file)) {
						File f = getFile(file);
						if (!f.isDirectory()) {
							error("Adding a sourcepath that is not a directory: " + f);
						} else {
							sourcePath.add(f);
						}
					}
				}
			}
		}
		return sourcePath;
	}

	private void addSources(Jar dot) {
		if (!hasSources())
			return;

		Set<PackageRef> packages = Create.set();

		for (TypeRef typeRef : getClassspace().keySet()) {
			PackageRef packageRef = typeRef.getPackageRef();
			String sourcePath = typeRef.getSourcePath();
			String packagePath = packageRef.getPath();

			boolean found = false;
			String[] fixed = {
					"packageinfo", "package.html", "module-info.java", "package-info.java"
			};

			for (Iterator<File> i = getSourcePath().iterator(); i.hasNext();) {
				File root = i.next();

				// TODO should use bcp?

				File f = getFile(root, sourcePath);
				if (f.exists()) {
					found = true;
					if (!packages.contains(packageRef)) {
						packages.add(packageRef);
						File bdir = getFile(root, packagePath);
						for (int j = 0; j < fixed.length; j++) {
							File ff = getFile(bdir, fixed[j]);
							if (ff.isFile()) {
								String name = "OSGI-OPT/src/" + packagePath + "/" + fixed[j];
								dot.putResource(name, new FileResource(ff));
							}
						}
					}
					if (packageRef.isDefaultPackage())
						System.err.println("Duh?");
					dot.putResource("OSGI-OPT/src/" + sourcePath, new FileResource(f));
				}
			}
			if (!found) {
				for (Jar jar : getClasspath()) {
					Resource resource = jar.getResource(sourcePath);
					if (resource != null) {
						dot.putResource("OSGI-OPT/src/" + sourcePath, resource);
					} else {
						resource = jar.getResource("OSGI-OPT/src/" + sourcePath);
						if (resource != null) {
							dot.putResource("OSGI-OPT/src/" + sourcePath, resource);
						}
					}
				}
			}
			if (getSourcePath().isEmpty())
				warning("Including sources but " + SOURCEPATH + " does not contain any source directories ");
			// TODO copy from the jars where they came from
		}
	}
