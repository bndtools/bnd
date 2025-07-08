---
layout: default
title: -nojunit  BOOLEAN
class: Ant
summary: |
   Indicate that this project does not have JUnit tests
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-nojunit=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/nojunit.md --><br /><br />

	private void checkForTesting(Project project, Properties properties) throws Exception {

		//
		// Only run junit when we have a test src directory
		//

		boolean junit = project.getTestSrc().isDirectory() && !Processor.isTrue(project.getProperty(Constants.NOJUNIT));
		boolean junitOsgi = project.getProperties().getProperty(Constants.TESTCASES) != null
				&& !Processor.isTrue(project.getProperty(Constants.NOJUNITOSGI));

		if (junit)
			properties.setProperty("project.junit", "true");
		if (junitOsgi)
			properties.setProperty("project.osgi.junit", "true");
	}
