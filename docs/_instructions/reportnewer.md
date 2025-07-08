---
layout: default
title: -reportnewer BOOLEAN
class: Project
summary: |
   Report any entries that were added to the build since the last JAR was made.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-reportnewer=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/reportnewer.md --><br /><br />



	private void reportNewer(long lastModified, Jar jar) {
		if (isTrue(getProperty(Constants.REPORTNEWER))) {
			StringBuilder sb = new StringBuilder();
			String del = "Newer than " + new Date(lastModified);
			for (Map.Entry<String,Resource> entry : jar.getResources().entrySet()) {
				if (entry.getValue().lastModified() > lastModified) {
					sb.append(del);
					del = ", \n     ";
					sb.append(entry.getKey());
				}
			}
			if (sb.length() > 0)
				warning(sb.toString());
		}
	}
