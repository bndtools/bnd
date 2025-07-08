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

### Options ###

- `visibility:`
  - Example: `visibility:=private`

  - Values: `private,reexport`

  - Pattern: `private|reexport`


- `resolution:`
  - Example: `resolution:=optional`

  - Values: `mandatory,optional`

  - Pattern: `mandatory|optional`


- `-split-package:`
  - Example: `-split-package:=merge-first`

  - Values: `merge-first,merge-last,error,first`

  - Pattern: `merge-first|merge-last|error|first`


- `bundle-version`
  - Example: `bundle-version=1.3`

  - Pattern: `((\(|\[)\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?,\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?(\]|\)))|\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?`

<!-- Manual content from: ext/require_bundle.md --><br /><br />
	
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
	
