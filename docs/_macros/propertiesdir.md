---
layout: default
title: 	propertiesdir 
class: 	Processor
summary: The directory of the properties file
---

	public String _propertiesdir(String[]args) {
		if ( args.length > 1) {
			error("propertiesdir does not take arguments");
			return null;
		}
		File pf = getPropertiesFile();
		if ( pf == null)
			return "";
		
		return pf.getParentFile().getAbsolutePath();
	}
