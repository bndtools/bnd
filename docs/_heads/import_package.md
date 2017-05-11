---
layout: default
class: Header
title: Import-Package ::= import ( ',' import )* 
summary: The Import-Package header declares the imported packages for this bundle. 
---
The Import-Package header lists the packages that are required by the contained packages. The default for this header is "*", resulting in importing all referred packages. This header therefore rarely has to be specified. However, in certain cases there is an unwanted import. The import is caused by code that the author knows can never be reached. This import can be removed by using a negating pattern. A pattern is inserted in the import as an extra import when it contains no wildcards and there is no referral to that package. This can be used to add an import statement for a package that is not referred to by your code but is still needed, for example, because the class is loaded by name.

For example:
  Import-Package: !org.apache.commons.log4j, com.acme.*,
     com.foo.extra

During processing, bnd will attempt to find the exported version of imported packages. If no version or version range is specified on the import instruction, the exported version will then be used though the micro part and the qualifier are dropped. That is, when the exporter is `1.2.3.build123`, then the import version will be 1.2. If a specific version (range) is specified, this will override any found version. This default an be overridden with the [-versionpolicy][#versionpolicy] command.

If an explicit version is given, then ${@} can be used to substitute the found version in a range. In those cases, the version macro can be very useful to calculate ranges or drop specific parts of the version. For example:

  Import-Package: org.osgi.framework;version="[1.3,2.0)"
  Import-Package: org.osgi.framework;version=${@}
  Import-Package: org.osgi.framework;version="[${versionmask;==;${@}},${versionmask;=+;${@}})"

Packages with directive resolution:=dynamic will be removed from Import-Package and added to the DynamicImport-Package header after being processed like any other Import-Package entry. For example:

  Import-Package: org.slf4j.*;resolution:=dynamic, *

If an imported package uses mandatory attributes, then bnd will attempt to add those attributes to the import statement. However, in certain (bizarre!) cases this is not wanted. It is therefore possible to remove an attribute from the import clause. This is done with the `-remove-attribute:` directive or by setting the value of an attribute to !. The parameter of the `-remove-attribute` directive is an instruction and can use the standard options with !, *, ?, etc.

  Import-Package: org.eclipse.core.runtime;-remove-attribute:common,*

Or

  Import-Package: org.eclipse.core.runtime;common=!,*

Directives that are not part of the OSGi specification will give a warning unless they are prefixed with a 'x-'.

	
			//
			// IMPORTS
			// Imports MUST come after exports because we use information from
			// the exports
			//
			{
				// Add all exports that do not have an -noimport: directive
				// to the imports.
				Packages referredAndExported = new Packages(referred);
				referredAndExported.putAll(doExportsToImports(exports));

				removeDynamicImports(referredAndExported);

				// Remove any Java references ... where are the closures???
				for (Iterator<PackageRef> i = referredAndExported.keySet().iterator(); i.hasNext();) {
					if (i.next().isJava())
						i.remove();
				}

				Set<Instruction> unused = Create.set();
				String h = getProperty(IMPORT_PACKAGE);
				if (h == null) // If not set use a default
					h = "*";

				if (isPedantic() && h.trim().length() == 0)
					warning("Empty " + Constants.IMPORT_PACKAGE + " header");

				Instructions filter = new Instructions(h);
				imports = filter(filter, referredAndExported, unused);
				if (!unused.isEmpty()) {
					// We ignore the end wildcard catch
					if (!(unused.size() == 1 && unused.iterator().next().toString().equals("*")))
						warning("Unused " + Constants.IMPORT_PACKAGE + " instructions: %s ", unused);
				}

				// See what information we can find to augment the
				// imports. I.e. look in the exports
				augmentImports(imports, exports);
			}
	
	
	
	
				// Checks
			//
			if (referred.containsKey(Descriptors.DEFAULT_PACKAGE)) {
				error("The default package '.' is not permitted by the " + Constants.IMPORT_PACKAGE + " syntax. \n"
						+ " This can be caused by compile errors in Eclipse because Eclipse creates \n"
						+ "valid class files regardless of compile errors.\n"
						+ "The following package(s) import from the default package "
						+ uses.transpose().get(Descriptors.DEFAULT_PACKAGE));
			}
	
	
			verifyDirectives(Constants.IMPORT_PACKAGE, "resolution:", PACKAGEPATTERN, "package");
	
	
		/**
	 * Verify that the imports properly use version ranges.
	 */
	private void verifyImports() {
		if (isStrict()) {
			Parameters map = parseHeader(manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE));
			Set<String> noimports = new HashSet<String>();
			Set<String> toobroadimports = new HashSet<String>();

			for (Entry<String,Attrs> e : map.entrySet()) {
				String version = e.getValue().get(Constants.VERSION_ATTRIBUTE);
				if (version == null) {
					if (!e.getKey().startsWith("javax.")) {
						noimports.add(e.getKey());
					}
				} else {
					if (!VERSIONRANGE.matcher(version).matches()) {
						Location location = error("Import Package %s has an invalid version range syntax %s",
								e.getKey(), version).location();
						location.header = Constants.IMPORT_PACKAGE;
						location.context = e.getKey();
					} else {
						try {
							VersionRange range = new VersionRange(version);
							if (!range.isRange()) {
								toobroadimports.add(e.getKey());
							}
							if (range.includeHigh() == false && range.includeLow() == false
									&& range.getLow().equals(range.getHigh())) {
								Location location = error(
										"Import Package %s has an empty version range syntax %s, likely want to use [%s,%s]",
										e.getKey(), version, range.getLow(), range.getHigh()).location();
								location.header = Constants.IMPORT_PACKAGE;
								location.context = e.getKey();
							}
							// TODO check for exclude low, include high?
						}
						catch (Exception ee) {
							Location location = error("Import Package %s has an invalid version range syntax %s:%s",
									e.getKey(), version, ee.getMessage()).location();
							location.header = Constants.IMPORT_PACKAGE;
							location.context = e.getKey();
						}
					}
				}
			}

			if (!noimports.isEmpty()) {
				Location location = error("Import Package clauses without version range (excluding javax.*): %s",
						noimports).location();
				location.header = Constants.IMPORT_PACKAGE;
			}
			if (!toobroadimports.isEmpty()) {
				Location location = error(
						"Import Package clauses which use a version instead of a version range. This imports EVERY later package and not as many expect until the next major number: %s",
						toobroadimports).location();
				location.header = Constants.IMPORT_PACKAGE;
			}
		}
	}

	
