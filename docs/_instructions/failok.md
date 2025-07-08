---
layout: default
title: -failok ('true' | 'false')?
class: Project
summary: |
   Will ignore any error during building and assume all went ok.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-failok=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/failok.md --><br /><br />
	
	@Override
	public boolean isFailOk() {
		String v = getProperty(Analyzer.FAIL_OK, null);
		return v != null && v.equalsIgnoreCase("true");
	}
