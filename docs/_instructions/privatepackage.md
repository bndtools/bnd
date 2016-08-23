---
layout: default
class: Builder
title: -privatepackage PACKAGE-SPEC 
summary: Specify the private packages, these packages are included from the class path. Alternative to Private-Package, this version is not included in the manifest.
---


	/**
	 * Allow any local initialization by subclasses before we build.
	 */
	public void init() throws Exception {
		begin();
		doRequireBnd();

		// Check if we have sensible setup

		if (getClasspath().size() == 0
				&& (getProperty(EXPORT_PACKAGE) != null || getProperty(EXPORT_PACKAGE) != null || getProperty(PRIVATE_PACKAGE) != null || getProperty(PRIVATEPACKAGE) != null))
			warning("Classpath is empty. " + Constants.PRIVATE_PACKAGE + " (-privatepackage) and " + EXPORT_PACKAGE + " can only expand from the classpath when there is one");

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
	