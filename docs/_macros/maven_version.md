---
layout: default
class: Builder
title: maven_version ';' MAVEN-VERSION
summary: Cleanup a potential maven version to make it match an OSGi Version syntax.
---


	/**
	 * A macro to convert a maven version to an OSGi version
	 */

	public String _maven_version(String args[]) {
		if (args.length > 2)
			error("${maven_version} macro receives too many arguments " + Arrays.toString(args));
		else if (args.length < 2)
			error("${maven_version} macro has no arguments, use ${maven_version;1.2.3-SNAPSHOT}");
		else {
			return cleanupVersion(args[1]);
		}
		return null;
	}

