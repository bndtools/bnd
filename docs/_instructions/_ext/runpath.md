---
layout: default
class: Project
title: -runpath REPO-ENTRY ( ',' REPO-ENTRY ) 
summary:  Additional JARs for the remote VM path, should include the framework.
---
An OSGi application will have a set of bundles and an environment created by the framework and any additional JARs on the class path. The `-runpath` instruction sets these additional bundles. These JARs can actually export packages and provide capabilities that the launcher will automatically add to the system capabilities. The resolver will do the same. Any packages exported by bundles or provided capabilities on the `-runpath` are automatically added to the system capabilities.

For example:

	-runpath: \
		com.foo.bar;version=1, \
		file.jar; version=file
