---
layout: default
class: Header
title: Export-Package  ::= export ( ',' export)* 
summary: The Export-Package header contains a declaration of exported packages
---
The bnd definition allows the specification to be done using ''patterns'', a modified regular expression. All patterns in the definition are matched against every package on the [ class path][#CLASSPATH ]. If the pattern is a negating pattern (starts with !) and it is matched, then the package is completely excluded. Normal patterns cause the package to be included in the resulting bundle. Patterns can include both directives and attributes, these items will be copied to the output. The list is ordered, earlier patterns take effect before later patterns. The following examples copies everything on the class path except for packages starting with `com`. The default for Export-Package is "*", which can result in quite large bundles. If the source packages have an associated version (from their manifest of packageinfo file), then this version is automatically added to the clauses.

  Export-Package= !com.*, *

Exports are automatically imported. This features can be disabled with a special directive on the export instruction: `-noimport:=true`. For example:
  
  Export-Package= com.acme.impl.*;-noimport:=true, *

Bnd will automatically calculate the `uses:` directive. This directive is used by the OSGi framework to create a consistent class space for a bundle. The Export-Package statement allows this directive to be overridden on a package basis by specifying the directive in an Export-Package instruction. 

  Export-package = com.acme.impl.*;uses="my.special.import"

However, in certain cases it is necessary to augment the uses clause. It is therefore possible to use the special name `<<USES>>` in the clause. Bnd will replace this special name with the calculated uses set. Bnd will remove any extraneous commas when the `<<USES>>` is empty.

  Export-package = com.acme.impl.*;uses:="my.special.import,<<USES>>"

Directives that are not part of the OSGi specification will give a warning unless they are prefixed with a 'x-'.


			//
			// EXPORTS
			//
			{
				Set<Instruction> unused = Create.set();

				Instructions filter = new Instructions(getExportPackage());
				filter.append(getExportContents());

				exports = filter(filter, contained, unused);

				if (!unused.isEmpty()) {
					warning("Unused " + Constants.EXPORT_PACKAGE + " instructions: %s ", unused);
				}

				// See what information we can find to augment the
				// exports. I.e. look on the classpath
				augmentExports(exports);
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
	
		/**
	 * Verify that the exports only use versions.
	 */
	private void verifyExports() {
		if (isStrict()) {
			Parameters map = parseHeader(manifest.getMainAttributes().getValue(Constants.EXPORT_PACKAGE));
			Set<String> noexports = new HashSet<String>();

			for (Entry<String,Attrs> e : map.entrySet()) {

				String version = e.getValue().get(Constants.VERSION_ATTRIBUTE);
				if (version == null) {
					noexports.add(e.getKey());
				} else {
					if (!VERSION.matcher(version).matches()) {
						Location location;
						if (VERSIONRANGE.matcher(version).matches()) {
							location = error(
									"Export Package %s version is a range: %s; Exports do not allow for ranges.",
									e.getKey(), version).location();
						} else {
							location = error("Export Package %s version has invalid syntax: %s", e.getKey(), version)
									.location();
						}
						location.header = Constants.EXPORT_PACKAGE;
						location.context = e.getKey();
					}
				}

				if (e.getValue().containsKey(Constants.SPECIFICATION_VERSION)) {
					Location location = error(
							"Export Package %s uses deprecated specification-version instead of version", e.getKey())
							.location();
					location.header = Constants.EXPORT_PACKAGE;
					location.context = e.getKey();
				}

				String mandatory = e.getValue().get(Constants.MANDATORY_DIRECTIVE);
				if (mandatory != null) {
					Set<String> missing = new HashSet<String>(split(mandatory));
					missing.removeAll(e.getValue().keySet());
					if (!missing.isEmpty()) {
						Location location = error("Export Package %s misses mandatory attribute: %s", e.getKey(),
								missing).location();
						location.header = Constants.EXPORT_PACKAGE;
						location.context = e.getKey();
					}
				}
			}

			if (!noexports.isEmpty()) {
				Location location = error("Export Package clauses without version range: %s", noexports).location();
				location.header = Constants.EXPORT_PACKAGE;
			}
		}
	}

	