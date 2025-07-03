---
layout: default
class: Project
title: -removeheaders KEY-SELECTOR ( '.' KEY-SELECTOR ) *
summary: Remove matching headers from the manifest. 
---

		// Remove all the headers mentioned in -removeheaders
			Instructions instructions = new Instructions(getProperty(REMOVEHEADERS));
			Collection<Object> result = instructions.select(main.keySet(), false);
			main.keySet().removeAll(result);

			
				@Override
	public Jar executable() throws Exception {
		
		// TODO use constants in the future
		Parameters packageHeader = OSGiHeader.parseHeader(project.getProperty("-package"));
		boolean useShas = packageHeader.containsKey("jpm");
		project.trace("Useshas %s %s", useShas, packageHeader);

		Jar jar = new Jar(project.getName());
		
		Builder b = new Builder();
		project.addClose(b);
		
		if (!project.getIncludeResource().isEmpty()) {
			b.setIncludeResource(project.getIncludeResource().toString());
			b.setProperty(Constants.RESOURCEONLY, "true");
			b.build();
			if ( b.isOk()) {
				jar.addAll(b.getJar());
			}
			project.getInfo(b);
		}
		
		List<String> runpath = getRunpath();

		Set<String> runpathShas = new LinkedHashSet<String>();
		Set<String> runbundleShas = new LinkedHashSet<String>();
		List<String> classpath = new ArrayList<String>();

		for (String path : runpath) {
			project.trace("embedding runpath %s", path);
			File file = new File(path);
			if (file.isFile()) {
				if (useShas) {
					String sha = SHA1.digest(file).asHex();
					runpathShas.add(sha + ";name=\"" + file.getName() + "\"");
				} else {
					String newPath = "jar/" + file.getName();
					jar.putResource(newPath, new FileResource(file));
					classpath.add(newPath);
				}
			}
		}

		// Copy the bundles to the JAR

		List<String> runbundles = (List<String>) getRunBundles();
		List<String> actualPaths = new ArrayList<String>();

		for (String path : runbundles) {
			project.trace("embedding run bundles %s", path);
			File file = new File(path);
			if (!file.isFile())
				project.error("Invalid entry in -runbundles %s", file);
			else {
				if (useShas) {
					String sha = SHA1.digest(file).asHex();
					runbundleShas.add(sha + ";name=\"" + file.getName() + "\"");
					actualPaths.add("${JPMREPO}/" + sha);
				} else {
					String newPath = "jar/" + file.getName();
					jar.putResource(newPath, new FileResource(file));
					actualPaths.add(newPath);
				}
			}
		}

		LauncherConstants lc = getConstants(actualPaths, true);
		lc.embedded = !useShas;
		lc.storageDir = null; // cannot use local info

		final Properties p = lc.getProperties();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		p.store(bout, "");
		jar.putResource(LauncherConstants.DEFAULT_LAUNCHER_PROPERTIES, new EmbeddedResource(bout.toByteArray(), 0L));

		Manifest m = new Manifest();
		Attributes main = m.getMainAttributes();

		for (Entry<Object,Object> e : project.getFlattenedProperties().entrySet()) {
			String key = (String) e.getKey();
			if (key.length() > 0 && Character.isUpperCase(key.charAt(0)))
				main.putValue(key, (String) e.getValue());
		}

		Instructions instructions = new Instructions(project.getProperty(Constants.REMOVEHEADERS));
		Collection<Object> result = instructions.select(main.keySet(), false);
		main.keySet().removeAll(result);

		if (useShas) {
			project.trace("Use JPM launcher");
			m.getMainAttributes().putValue("Main-Class", JPM_LAUNCHER_FQN);
			m.getMainAttributes().putValue("JPM-Classpath", Processor.join(runpathShas));
			m.getMainAttributes().putValue("JPM-Runbundles", Processor.join(runbundleShas));
			URLResource jpmLauncher = new URLResource(this.getClass().getResource("/" + JPM_LAUNCHER));
			jar.putResource(JPM_LAUNCHER, jpmLauncher);
		} else {
			project.trace("Use Embedded launcher");
			m.getMainAttributes().putValue("Main-Class", EMBEDDED_LAUNCHER_FQN);
			m.getMainAttributes().putValue(EmbeddedLauncher.EMBEDDED_RUNPATH, Processor.join(classpath));
			URLResource embeddedLauncher = new URLResource(this.getClass().getResource("/" + EMBEDDED_LAUNCHER));
			jar.putResource(EMBEDDED_LAUNCHER, embeddedLauncher);
		}
		if ( project.getProperty(Constants.DIGESTS) != null)
			jar.setDigestAlgorithms(project.getProperty(Constants.DIGESTS).trim().split("\\s*,\\s*"));
		else
			jar.setDigestAlgorithms(new String[]{"SHA-1", "MD-5"});
		jar.setManifest(m);
		return jar;
	}

			