---
layout: default
class: Header
title: Bundle-ClassPath ::= entry ( ',' entry )*
summary: The Bundle-ClassPath header defines a comma-separated list of JAR file path names or directories (inside the bundle) containing classes and resources. The full stop ('.' \u002E) specifies the root di- rectory of the bundle's JAR. The full stop is also the default
---
	
Defines the internal bundle class path, is taken into accont by bnd. That is, classes will be analyzed according to this path. The files/directories on the Bundle-ClassPath must be present in the bundle. Use Include-Resource to include these jars/directories in your bundle. In general you should not use Bundle-ClassPath since it makes things more complicated than necessary. Use the @ option in the Include-Resource to unroll the jars into the JAR.


	public void verifyBundleClasspath() {
		Parameters bcp = main.getBundleClassPath();
		if (bcp.isEmpty() || bcp.containsKey("."))
			return;

		for (String path : bcp.keySet()) {
			if (path.endsWith("/"))
				error("A " + Constants.BUNDLE_CLASSPATH + " entry must not end with '/': %s", path);

			if (dot.getDirectories().containsKey(path))
				// We assume that any classes are in a directory
				// and therefore do not care when the bundle is included
				return;
		}

		for (String path : dot.getResources().keySet()) {
			if (path.endsWith(".class")) {
				warning("The " + Constants.BUNDLE_CLASSPATH + " does not contain the actual bundle JAR (as specified with '.' in the " + Constants.BUNDLE_CLASSPATH + ") but the JAR does contain classes. Is this intentional?");
				return;
			}
		}
	}

	
	
	
		/**
	 * Check for unresolved imports. These are referrals that are not imported
	 * by the manifest and that are not part of our bundle class path. The are
	 * calculated by removing all the imported packages and contained from the
	 * referred packages.
	 * @throws Exception 
	 */
	private void verifyUnresolvedReferences() throws Exception {

		//
		// If we're being called from the builder then this should
		// already have been done
		//

		if (isFrombuilder())
			return;

		Manifest m = analyzer.getJar().getManifest();
		if (m == null) {
			error("No manifest");
		}

		Domain domain = Domain.domain(m);
		
		Set<PackageRef> unresolvedReferences = new TreeSet<PackageRef>(analyzer.getReferred().keySet());
		unresolvedReferences.removeAll(analyzer.getContained().keySet());
		for ( String pname : domain.getImportPackage().keySet()) {
			PackageRef pref = analyzer.getPackageRef(pname);
			unresolvedReferences.remove(pref);
		}

		// Remove any java.** packages.
		for (Iterator<PackageRef> p = unresolvedReferences.iterator(); p.hasNext();) {
			PackageRef pack = p.next();
			if (pack.isJava())
				p.remove();
			else {
				// Remove any dynamic imports
				if (isDynamicImport(pack))
					p.remove();
			}
		}

		//
		// If there is a Require bundle, all bets are off and
		// we cannot verify anything
		//

		if (domain.getRequireBundle().isEmpty() && domain.get("ExtensionBundle-Activator") == null
				&& (domain.getFragmentHost()== null || domain.getFragmentHost().getKey().equals("system.bundle"))) {

			if (!unresolvedReferences.isEmpty()) {
				// Now we want to know the
				// classes that are the culprits
				Set<String> culprits = new HashSet<String>();
				for (Clazz clazz : analyzer.getClassspace().values()) {
					if (hasOverlap(unresolvedReferences, clazz.getReferred()))
						culprits.add(clazz.getAbsolutePath());
				}

				if (analyzer instanceof Builder)
					warning("Unresolved references to %s by class(es) %s on the " + Constants.BUNDLE_CLASSPATH + ": %s",
							unresolvedReferences, culprits, analyzer.getBundleClasspath().keySet());
				else
					error("Unresolved references to %s by class(es) %s on the " + Constants.BUNDLE_CLASSPATH + ": %s",
							unresolvedReferences, culprits, analyzer.getBundleClasspath().keySet());
				return;
			}
		} else if (isPedantic())
			warning("Use of " + Constants.REQUIRE_BUNDLE + ", ExtensionBundle-Activator, or a system bundle fragment makes it impossible to verify unresolved references");
	}
	
	
	
	
						if (dot.getDirectories().containsKey(path)) {
						// if directories are used, we should not have dot as we
						// would have the classes in these directories on the
						// class path twice.
						if (bcp.containsKey("."))
							warning(Constants.BUNDLE_CLASSPATH
									+ " uses a directory '%s' as well as '.'. This means bnd does not know if a directory is a package.",
									path, path);
						analyzeJar(dot, Processor.appendPath(path) + "/", true);
					} else {
						if (!"optional".equals(info.get(RESOLUTION_DIRECTIVE)))
							warning("No sub JAR or directory " + path);
					}
	
	
				Parameters bcp = getBundleClasspath();
			if (bcp.isEmpty() || (bcp.containsKey(".") && bcp.size() == 1))
				main.remove(BUNDLE_CLASSPATH);
			else
				main.putValue(BUNDLE_CLASSPATH, printClauses(bcp));

			// ----- Require/Capabilities section
	