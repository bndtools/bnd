---
layout: default
title: 	propertiesname 
class: 	Project
summary: Return the name of the properties file
---

	public String _propertiesname(String[]args) {
		File pf = getPropertiesFile();
		if ( pf == null)
			return "";
		
		return pf.getName();
	}

