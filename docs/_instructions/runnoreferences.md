---
layout: default
class: Launcher
title: -runnoreferences  BOOLEAN
summary: Do not use the reference url for installing a bundle in the installer. This is the default for windows because it is quite obstinate about open files, on other platforms the more efficient reference urls are used. 
---

	private LauncherConstants getConstants(Collection<String> runbundles, boolean exported) throws Exception, FileNotFoundException,
			IOException {
		project.trace("preparing the aQute launcher plugin");

		LauncherConstants lc = new LauncherConstants();
		lc.noreferences = Processor.isTrue(project.getProperty(Constants.RUNNOREFERENCES));
		lc.runProperties = getRunProperties();
		lc.storageDir = getStorageDir();
		lc.keep = isKeep();
		lc.runbundles.addAll(runbundles);
		lc.trace = getTrace();

		
			Bundle install(File f) throws Exception {
		BundleContext context = systemBundle.getBundleContext();
		try {
			String reference;
			if (isWindows() || parms.noreferences) {
				trace("no reference: url %s", parms.noreferences);
				reference = f.toURI().toURL().toExternalForm();
			} else
				reference = "reference:" + f.toURI().toURL().toExternalForm();

			Bundle b = context.installBundle(reference);
			if (b.getLastModified() < f.lastModified()) {
				b.update();
			}
			return b;
		}
		catch (BundleException e) {
			trace("failed reference, will try to install %s with input stream", f.getAbsolutePath());
			String reference = f.toURI().toURL().toExternalForm();
			InputStream in = new FileInputStream(f);
			try {
				return context.installBundle(reference, in);
			}
			finally {
				in.close();
			}
		}
	}

		