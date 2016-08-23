---
layout: default
class: Launcher
title: -runproperties PROPERTIES 
summary:  Define system properties for the remote VM.
---

	public Map<String,String> getRunProperties() {
		return OSGiHeader.parseProperties(getProperty(RUNPROPERTIES));
	}

	public Launcher(Properties properties, final File propertiesFile) throws Exception {
		this.properties = properties;

		// Allow the system to override any properties with -Dkey=value

		for (Object key : properties.keySet()) {
			String s = (String) key;
			String v = System.getProperty(s);
			if (v != null)
				properties.put(key, v);
		}


		System.getProperties().putAll(properties);
		
		
		this.parms = new LauncherConstants(properties);
		out = System.err;
