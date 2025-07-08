---
layout: default
class: Header
title: Require-Bundle      ::= bundle-description ( ',' bundle-description )* 
summary: The Require-Bundle header specifies that all exported packages from another bundle must be im- ported, effectively requiring the public interface of another bundle. 
---
	
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
	