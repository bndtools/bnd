---
layout: bnd
title: -buildpath PATH
class: Project
summary: |
   Provides the class path for building the jar, the entries are references to the repositories.
parent: Instruction Reference
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-buildpath=osgi;version=4.1`

- Values: `${repo;bsns}`

- Pattern: `[-\w]+(:?\.[-\w]+)*`

### Directives 

- `version` Specifies the range in the repository, project or file.
  - Example: `version=project`

  - Values: `project,type`

  - Pattern: `project|type|((\(|\[)\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?,\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?(\]|\)))|\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?`

<!-- Manual content from: ext/buildpath.md --><br /><br />

The `-buildpath` instruction is the main mechanism to add build-time dependencies to a project. A dependency is either another project in the workspace, or a bundle in a repository. The `-buildpath` is only used during compile and build time; it is never used to run projects.  Because `-buildpath` dependencies are only used compile time it's recommended to add bundles containing only APIs; you don't need bundles containing implementations.

## Examples

An example of the `-buildpath` could be the following, where three dependencies are defined:

    -buildpath: \
     some.other.workspace.project;version=project,\
     osgi.core;version=4.3.1,\
     osgi.cmpn;version=4.3.1

## Eclipse Features

An entry can reference an Eclipse feature from a [p2 repository](/plugins/p2repo.html) by adding the `type=org.eclipse.update.feature` attribute. The `type` attribute is required because a feature can share its name and version with a bundle; without it the entry is resolved as a bundle. A project of the workspace is never taken for a feature, and the feature has to be given by its exact id, a wildcard cannot be used.

    -buildpath: \
     org.eclipse.e4.rcp;version='4.40.0.v20260516-1214';type=org.eclipse.update.feature

A feature is a container of bundles: on the build path it expands to its member bundles, i.e. the `<plugin>` references of its `feature.xml` and, recursively, the members of its `<includes>` referenced features. `<requires>` imports are dependencies, not members, and are not expanded. Members whose `os`/`ws`/`arch` attributes do not match the target platform are skipped. The target platform is given by `osgi.os`, `osgi.ws` and `osgi.arch` in the [-runproperties] of the project; a value that is not set defaults to the platform bnd runs on. The versions pinned in the feature are resolved exactly; when a pinned version is not present in the repositories, the highest available version is used and a warning is issued.

Feature entries are supported on all container paths, i.e. also on `-testpath`, `-runpath` and `-runbundles`. To let the resolver select the bundles of a feature, use the feature in the [-runrequires] instead. A feature can only be expanded when its members are known to the repository; the index of a [p2 repository](/plugins/p2repo.html#eclipse-features) that was created by an earlier bnd version is updated automatically.

## See Also

* [-testpath] for setting the test path.
* [-runrequires] for resolving the bundles of an Eclipse feature.
* [-runproperties] for the platform of an Eclipse feature.
* [-maven-dependencies] for how to configure maven dependency information.

[-testpath]: testpath.html
[-runrequires]: runrequires.html#eclipse-features
[-runproperties]: runproperties.html
[-maven-dependencies]: maven-dependencies.html
