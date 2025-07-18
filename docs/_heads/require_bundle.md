---
layout: default
title: Require-Bundle      ::= bundle-description ( ',' bundle-description )*
class: Header
summary: |
   The Require-Bundle header specifies that all exported packages from another bundle must be im- ported, effectively requiring the public interface of another bundle.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Require-Bundle: com.acme.chess`

- Pattern: `.*`

### Options 

- `visibility:` If the value is private (Default), then all visible packages from the required bundles are not re-exported. If the value is reexport then bundles that require this bundle will transitively have access to these required bundle’s exported packages.
  - Example: `visibility:=private`

  - Values: `private,reexport`

  - Pattern: `private|reexport`


- `resolution:` If the value is mandatory (default) then the required bundle must exist for this bundle to resolve. If the value is optional, the bundle will resolve even if the required bundle does not exist.
  - Example: `resolution:=optional`

  - Values: `mandatory,optional`

  - Pattern: `mandatory|optional`


- `-split-package:` Indicates how an imported package should be merged when it is split between different exporters. The default is merge-first with warning.
  - Example: `-split-package:=merge-first`

  - Values: `merge-first,merge-last,error,first`

  - Pattern: `merge-first|merge-last|error|first`


- `bundle-version` A version range to select the bundle version of the exporting bundle. The default value is 0.0.0.
  - Example: `bundle-version=1.3`

  - Pattern: `((\(|\[)\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?,\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?(\]|\)))|\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?`

<!-- Manual content from: ext/require_bundle.md --><br /><br />

# Require-Bundle

The `Require-Bundle` header specifies that the bundle requires all exported packages from another bundle. This effectively imports the public interface of the required bundle, making its packages available to the requiring bundle.

Example:

```
Require-Bundle: com.example.otherbundle
```

This header is less common in OSGi and can make dependency management more complex. Prefer using `Import-Package` when possible.


		verifyDirectives(Constants.REQUIRE_BUNDLE, "visibility:|resolution:", SYMBOLICNAME, "bsn");
	
	
	
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


TODO Needs review - AI Generated content
