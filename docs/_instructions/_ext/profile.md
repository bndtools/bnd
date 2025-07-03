---
layout: default
class: Builder
title: -profile KEY   
summary:  Sets a prefix that is used when a variable is not found, it is then re-searched under "[<[profile]>]<[key]>". 
---

	public Jar pack(String profile) throws Exception {
		Collection< ? extends Builder> subBuilders = getSubBuilders();

		if (subBuilders.size() != 1) {
			error("Project has multiple bnd files, please select one of the bnd files");
			return null;
		}

		Builder b = subBuilders.iterator().next();

		ignore.remove(BUNDLE_SYMBOLICNAME);
		ignore.remove(BUNDLE_VERSION);
		ignore.add(SERVICE_COMPONENT);

		ProjectLauncher launcher = getProjectLauncher();
		launcher.getRunProperties().put("profile", profile); // TODO remove
		launcher.getRunProperties().put(PROFILE, profile);
		Jar jar = launcher.executable();
		Manifest m = jar.getManifest();
		Attributes main = m.getMainAttributes();
		for (String key : getPropertyKeys(true)) {
			if (Character.isUpperCase(key.charAt(0)) && !ignore.contains(key)) {
				main.putValue(key, getProperty(key));
			}
		}

		if (main.getValue(BUNDLE_SYMBOLICNAME) == null)
			main.putValue(BUNDLE_SYMBOLICNAME, b.getBsn());

		if (main.getValue(BUNDLE_SYMBOLICNAME) == null)
			main.putValue(BUNDLE_SYMBOLICNAME, getName());

		if (main.getValue(BUNDLE_VERSION) == null) {
			main.putValue(BUNDLE_VERSION, Version.LOWEST.toString());
			warning("No version set, uses 0.0.0");
		}

		jar.setManifest(m);
		jar.calcChecksums(new String[] {
				"SHA1", "MD5"
		});
		return jar;
	}

	
			// Prevent recursion, but try to get a profiled variable
		if (key != null && !key.startsWith("[") && !key.equals(Constants.PROFILE)) {
			if (profile == null)
				profile = domain.get(Constants.PROFILE);
			if (profile != null) {
				String replace = getMacro("[" + profile + "]" + key, link);
				if (replace != null)
					return replace;
			}
		}
		return null;
	
