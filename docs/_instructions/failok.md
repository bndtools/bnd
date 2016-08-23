---
layout: default
class: Project
title: -failok ('true' | 'false')?
summary: Will ignore any error during building and assume all went ok. 
---
	
	@Override
	public boolean isFailOk() {
		String v = getProperty(Analyzer.FAIL_OK, null);
		return v != null && v.equalsIgnoreCase("true");
	}
