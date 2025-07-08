---
layout: default
class: Builder
title: -sources  BOOLEAN
summary: Include the source code (if available on the -sourcepath) in the bundle at OSGI-OPT/src 
---

	public Resource make(Builder builder, String destination, Map<String,String> argumentsOnMake) throws Exception {
		String type = argumentsOnMake.get("type");
		if (!"bnd".equals(type))
			return null;

		String recipe = argumentsOnMake.get("recipe");
		if (recipe == null) {
			builder.error("No recipe specified on a make instruction for " + destination);
			return null;
		}
		File bndfile = builder.getFile(recipe);
		if (bndfile.isFile()) {
			// We do not use a parent because then we would
			// build ourselves again. So we can not blindly
			// inherit the properties.
			Builder bchild = builder.getSubBuilder();
			bchild.removeBundleSpecificHeaders();

			// We must make sure that we do not include ourselves again!
			bchild.setProperty(Analyzer.INCLUDE_RESOURCE, "");
			bchild.setProperty(Analyzer.INCLUDERESOURCE, "");
			bchild.setProperties(bndfile, builder.getBase());

			Jar jar = bchild.build();
			Jar dot = builder.getTarget();

			if (builder.hasSources()) {
				for (String key : jar.getResources().keySet()) {
					if (key.startsWith("OSGI-OPT/src"))
						dot.putResource(key, jar.getResource(key));
				}
			}
			builder.getInfo(bchild, bndfile.getName() + ": ");
			return new JarResource(jar);
		}
		return null;
	}

		/**
     *
     */
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
	
		/**
	 * Cop
	 *
	 * @param dest
	 * @param srce
	 * @param path
	 * @param overwriteResource
	 */
	private void copy(Jar dest, Jar srce, String path, boolean overwrite) {
		trace("copy d=" + dest + " s=" + srce + " p=" + path);
		dest.copy(srce, path, overwrite);

		// bnd.info sources must be preprocessed
		String bndInfoPath = path + "/bnd.info";
		Resource r = dest.getResource(bndInfoPath);
		if (r != null && !(r instanceof PreprocessResource)) {
			trace("preprocessing bnd.info");
			PreprocessResource pp = new PreprocessResource(this, r);
			dest.putResource(bndInfoPath, pp);
		}

		if (hasSources()) {
			String srcPath = "OSGI-OPT/src/" + path;
			Map<String,Resource> srcContents = srce.getDirectories().get(srcPath);
			if (srcContents != null) {
				dest.addDirectory(srcContents, overwrite);
			}
		}
	}

	