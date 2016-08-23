---
layout: default
class: Workspace
title: -systemproperties PROPERTIES 
summary: These system properties are set in the local JVM when a workspace is started. This was mainly added to allow one to set JVM options via system properties.  
---

	public Workspace(File dir, String bndDir) throws Exception {
		super(getDefaults());
		dir = dir.getAbsoluteFile();
		if (!dir.exists() && !dir.mkdirs()) {
			throw new IOException("Could not create directory " + dir);
		}
		assert dir.isDirectory();

		File buildDir = new File(dir, bndDir).getAbsoluteFile();
		if (!buildDir.isDirectory())
			buildDir = new File(dir, CNFDIR).getAbsoluteFile();

		this.buildDir = buildDir;

		File buildFile = new File(buildDir, BUILDFILE).getAbsoluteFile();
		if (!buildFile.isFile())
			warning("No Build File in " + dir);

		setProperties(buildFile, dir);
		propertiesChanged();

		//
		// There is a nasty bug/feature in Java that gives errors on our
		// SSL use of github. The flag jsse.enableSNIExtension should be set
		// to false. So here we provide a way to set system properties
		// as early as possible
		//

		Attrs sysProps = OSGiHeader.parseProperties(getProperty(SYSTEMPROPERTIES));
		for (Entry<String,String> e : sysProps.entrySet()) {
			System.setProperty(e.getKey(), e.getValue());
		}

	}