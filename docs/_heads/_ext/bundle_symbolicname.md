---
layout: default
class: Header
title: Bundle-SymbolicName ::= symbolic-name ( ';' parameter ) * 
summary: The Bundle-SymbolicName header specifies a non-localizable name for this bundle. The bundle symbolic name together with a version must identify a unique bundle though it can be installed multiple times in a framework. The bundle symbolic name should be based on the reverse domain name convention, s 
---

# Bundle-SymbolicName

The `Bundle-SymbolicName` header specifies a unique, non-localizable name for the bundle. This name, together with the version, must uniquely identify a bundle, even if it is installed multiple times in a framework. The symbolic name should follow the reverse domain name convention (e.g., `com.example.mybundle`).

If not set, bnd will use the name of the main bnd file or, if the file is called `bnd.bnd`, the name of its directory. The `${project}` variable can also be used to set this value.

Example:

```
Bundle-SymbolicName: com.example.mybundle
```

This header is required for all OSGi bundles.
	
The Bundle-SymbolicName header can be set by the user. The default is the name of the main bnd file, or if the main bnd file is called bnd.bnd, it will be the name of the directory of the bnd file. An interesting variable is ${project} that will be set to this default name.


			private void verifySymbolicName() {
		Parameters bsn = parseHeader(main.get(Analyzer.BUNDLE_SYMBOLICNAME));
		if (!bsn.isEmpty()) {
			if (bsn.size() > 1)
				error("More than one BSN specified " + bsn);

			String name = bsn.keySet().iterator().next();
			if (!isBsn(name)) {
				error("Symbolic Name has invalid format: " + name);
			}
		}
	}
		
			/**
	 * @param name
	 * @return
	 */
	public static boolean isBsn(String name) {
		return SYMBOLICNAME.matcher(name).matches();
	}

		
	public final static String	SYMBOLICNAME_STRING				= "[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*";


---
TODO Needs review - AI Generated content