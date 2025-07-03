---
layout: default
title: -runpath REPO-ENTRY ( ',' REPO-ENTRY )
class: Project
summary: |
   Additional JARs for the remote VM path, should include the framework.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runpath=org.eclipse.osgi;version=3.5`

- Pattern: `.*`

### Directives ###

- `version`
  - Example: `version=project`

  - Values: `project,type`

  - Pattern: `project|type|((\(|\[)\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?,\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?(\]|\)))|\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?`

<!-- Manual content from: ext/runpath.md --><br /><br />
An OSGi application will have a set of bundles and an environment created by the framework and any additional JARs on the class path. The `-runpath` instruction sets these additional bundles. These JARs can actually export packages and provide capabilities that the launcher will automatically add to the system capabilities. The resolver will do the same. Any packages exported by bundles or provided capabilities on the `-runpath` are automatically added to the system capabilities.

For example:

	-runpath: \
		com.foo.bar;version=1, \
		file.jar; version=file
