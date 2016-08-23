---
layout: default
class: Header
title: Private-Package PACKAGE-SPEC ( ',' PACKAGE-SPEC )* 
summary: Specifies what packages to include
---
## Private Package
The method of inclusion is identical to the Export-Package header, the only difference is, is that these packages are not exported. This header will be copied to the manifest. If a package is selected by noth the export and private package headers, then the export takes precedence.

  Private-Package= com.*



###Split packages
Bnd traverse the packages on the classpath and copies them to the output based on the instructions given by the Export-Package and Private-Package headers. This opens up for the possibility that there are multiple packages with the same name on the class path. It is better to avoid this situation because it means there is no cohesive definition of the package and it is just, eh, messy. However, there are valid cases that packages should be merged from different sources. For example, when a standard package needs to be merged with implementation code like the osgi packages sometimes (unfortunately) do. Without any extra instructions, bnd will merge multiple packages where the last one wins if the packages contain duplicate resources, but it will give a warning to notify the unwanted case of split packages.

The `-split-package:` directive on the Export-Package/Private-Package clause allows fine grained control over what should be done with split packages. The following values are architected:

||`merge-first`||Merge split packages but do not add resources that come later in the classpath. That is, the first resource wins. This is the default, although the default will generate a warning||
||`merge-last`||Merge split packages but overwrite resources that come earlier in the classpath. That is, the last resource wins.||
||`first`||Do not merge, only use the first package found||
||`error`||Generate an error when a split package is detected||

For example:

  Private-Package: test.pack;-split-package:=merge-first






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
	