---
layout: default
title: -runproperties PROPERTIES
class: Launcher
summary: |
   Define system properties for the remote VM.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runproperties= foo=3, bar=4`

- Pattern: `.*`

<!-- Manual content from: ext/runproperties.md --><br /><br />

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
