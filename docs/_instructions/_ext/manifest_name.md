---
layout: default
class: Ant
title: -manifest-name  RESOURCE
summary:  Set the resource path to the manifest, for certain standards the manifest has a different name.
---

	public Jar build() throws Exception {
		trace("build");
		init();
		if (isTrue(getProperty(NOBUNDLES)))
			return null;

		if (getProperty(CONDUIT) != null)
			error("Specified " + CONDUIT + " but calls build() instead of builds() (might be a programmer error");

		Jar dot = new Jar("dot");
		try {
			long modified = Long.parseLong(getProperty("base.modified"));
			dot.updateModified(modified, "Base modified");
		}
		catch (Exception e) {
			// Ignore
		}
		setJar(dot);

		doExpand(dot);
		doIncludeResources(dot);
		doWab(dot);

		// Check if we override the calculation of the
		// manifest. We still need to calculated it because
		// we need to have analyzed the classpath.

		Manifest manifest = calcManifest();

		String mf = getProperty(MANIFEST);
		if (mf != null) {
			File mff = getFile(mf);
			if (mff.isFile()) {
				try {
					InputStream in = new FileInputStream(mff);
					manifest = new Manifest(in);
					in.close();
				}
				catch (Exception e) {
					error(MANIFEST + " while reading manifest file", e);
				}
			} else {
				error(MANIFEST + ", no such file " + mf);
			}
		}

		if ( !isTrue(getProperty(NOMANIFEST))) {
			dot.setManifest(manifest);
			String manifestName = getProperty(MANIFEST_NAME);
			if (manifestName != null)
				dot.setManifestName(manifestName);
		} else {
			dot.setDoNotTouchManifest();
		}

		// This must happen after we analyzed so
		// we know what it is on the classpath
		addSources(dot);

		if (getProperty(POM) != null)
			dot.putResource("pom.xml", new PomResource(dot.getManifest()));

		if (!isNoBundle())
			doVerify(dot);

		if (dot.getResources().isEmpty())
			warning("The JAR is empty: The instructions for the JAR named %s did not cause any content to be included, this is likely wrong",
					getBsn());

		dot.updateModified(lastModified(), "Last Modified Processor");
		dot.setName(getBsn());

		doDigests(dot);

		sign(dot);
		doSaveManifest(dot);

		doDiff(dot); // check if need to diff this bundle
		doBaseline(dot); // check for a baseline

		String expand = getProperty("-expand");
		if ( expand != null) {
			File out = getFile(expand);
			out.mkdirs();
			dot.expand(out);
		}
		return dot;
	}
