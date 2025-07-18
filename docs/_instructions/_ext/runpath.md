---
layout: default
class: Project
title: -runpath REPO-ENTRY ( ',' REPO-ENTRY ) 
summary:  Additional JARs for the remote VM path, should include the framework.
---
An OSGi application will have a set of bundles and an environment created by the framework and any additional JARs on the class path. The `-runpath` instruction sets these additional bundles. These JARs can actually export packages and provide capabilities that the launcher will automatically add to the system capabilities. The resolver will do the same. Any packages exported by bundles or provided capabilities on the `-runpath` are automatically added to the system capabilities.

The `-runpath` instruction allows you to specify additional JARs that should be included on the class path of the remote VM when running an OSGi application. These JARs can export packages and provide capabilities that are automatically added to the system capabilities, making them available to the resolver and the launcher.

This is particularly useful for including frameworks or other dependencies that are not part of the main set of bundles but are required for the application to function correctly.

For example:

	-runpath: \
		com.foo.bar;version=1, \
		file.jar; version=file


---
TODO Needs review - AI Generated content