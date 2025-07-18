---
layout: default
class: Header
title: Bundle-Version ::= version 
summary: The Bundle-SymbolicName header specifies a non-localizable name for this bundle. The bundle symbolic name together with a version must identify a unique bundle though it can be installed multiple times in a framework. The bundle symbolic name should be based on the reverse domain name convention.
---

# Bundle-Version

The `Bundle-Version` header specifies the version of the bundle. If this header is not provided, a default version of `0` will be set. The version must follow the OSGi versioning scheme: `major.minor.micro.qualifier`.

Example:

```
Bundle-Version: 1.2.3
```

This header is important for managing updates and dependencies between bundles.

The version of the bundle. If no such header is provided, a version of 0 will be set.	
	
	
	
		verifyHeader(Constants.BUNDLE_VERSION, VERSION, true);
			public final static Pattern	VERSION							= Pattern.compile(VERSION_STRING);
			public final static String	VERSION_STRING					= "[0-9]{1,9}(\\.[0-9]{1,9}(\\.[0-9]{1,9}(\\.[0-9A-Za-z_-]+)?)?)?";
		
		
			/**
	 * Intercept the call to analyze and cleanup versions after we have analyzed
	 * the setup. We do not want to cleanup if we are going to verify.
	 */

	@Override
	public void analyze() throws Exception {
		super.analyze();
		cleanupVersion(getImports(), null);
		cleanupVersion(getExports(), getVersion());
		String version = getProperty(BUNDLE_VERSION);
		if (version != null) {
			version = cleanupVersion(version);
			if (version.endsWith(".SNAPSHOT")) {
				version = version.replaceAll("SNAPSHOT$", getProperty(SNAPSHOT, "SNAPSHOT"));
			}
			setProperty(BUNDLE_VERSION, version);
		}
	}
		
		
					if (main.getValue(BUNDLE_VERSION) == null)
				main.putValue(BUNDLE_VERSION, "0");


---
TODO Needs review - AI Generated content