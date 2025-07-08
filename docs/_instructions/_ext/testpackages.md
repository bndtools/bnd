---
layout: default
class: Project
title: -testpackages PACKAGE-SPEC ( ',' PACKAGE-SPEC ) 
summary:  
---

	private void doExpand(Jar dot) {

		// Build an index of the class path that we can then
		// use destructively
		MultiMap<String,Jar> packages = new MultiMap<String,Jar>();
		for (Jar srce : getClasspath()) {
			dot.updateModified(srce.lastModified, srce + " (" + srce.lastModifiedReason + ")");
			for (Entry<String,Map<String,Resource>> e : srce.getDirectories().entrySet()) {
				if (e.getValue() != null)
					packages.add(e.getKey(), srce);
			}
		}

		Parameters privatePackages = getPrivatePackage();
		if (isTrue(getProperty(Constants.UNDERTEST))) {
			String h = getProperty(Constants.TESTPACKAGES, "test;presence:=optional");
			privatePackages.putAll(parseHeader(h));
		}

		if (!privatePackages.isEmpty()) {
			Instructions privateFilter = new Instructions(privatePackages);
			Set<Instruction> unused = doExpand(dot, packages, privateFilter);

			if (!unused.isEmpty()) {
				warning("Unused " + Constants.PRIVATE_PACKAGE + " instructions, no such package(s) on the class path: %s", unused);
			}
		}

		Parameters exportedPackage = getExportPackage();
		if (!exportedPackage.isEmpty()) {
			Instructions exportedFilter = new Instructions(exportedPackage);

			// We ignore unused instructions for exports, they should show
			// up as errors during analysis. Otherwise any overlapping
			// packages with the private packages should show up as
			// unused

			doExpand(dot, packages, exportedFilter);
		}
	}

		/**
	 * Check if the given resource is in scope of this bundle. That is, it
	 * checks if the Include-Resource includes this resource or if it is a class
	 * file it is on the class path and the Export-Package or Private-Package
	 * include this resource.
	 *
	 * @param f
	 * @return
	 */
	public boolean isInScope(Collection<File> resources) throws Exception {
		Parameters clauses = parseHeader(getProperty(Constants.EXPORT_PACKAGE));
		clauses.putAll(parseHeader(getProperty(Constants.PRIVATE_PACKAGE)));
		clauses.putAll(parseHeader(getProperty(Constants.PRIVATEPACKAGE)));
		if (isTrue(getProperty(Constants.UNDERTEST))) {
			clauses.putAll(parseHeader(getProperty(Constants.TESTPACKAGES, "test;presence:=optional")));
		}

		Collection<String> ir = getIncludedResourcePrefixes();

		Instructions instructions = new Instructions(clauses);

		for (File r : resources) {
			String cpEntry = getClasspathEntrySuffix(r);

			if (cpEntry != null) {

				if (cpEntry.equals("")) // Meaning we actually have a CPE
					return true;

				String pack = Descriptors.getPackage(cpEntry);
				Instruction i = matches(instructions, pack, null, r.getName());
				if (i != null)
					return !i.isNegated();
			}

			// Check if this resource starts with one of the I-C header
			// paths.
			String path = r.getAbsolutePath();
			for (String p : ir) {
				if (path.startsWith(p))
					return true;
			}
		}
		return false;
	}
	