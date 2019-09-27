---
layout: default
class: Project
title: -builderignore PATH-SPEC ( ',' PATH-SPEC ) *
summary: List of project-relative directories to be ignored by the builder.
---

Specified paths must be relative to the project. Each path represents a directory in a project to be ignored by the builder when deciding if the bundles of the project need to be built. This is processed for workspace model builds by the Bndtools builder in Eclipse and the Bnd Gradle plugin.

This can be useful when the workspace is configured to use different output folders for Bndtools in Eclipse and for Gradle. For example:

    bin: ${if;${driver;gradle};build/classes/java/main;bin}
    testbin: ${if;${driver;gradle};build/classes/java/test;bin_test}
    target-dir: ${if;${driver;gradle};build/libs;generated}

When configuring the workspace to use different output folders for Bndtools in Eclipse and for Gradle, you should also use `-builderignore` to instruct the builder to ignore the other builder's output folders.

    -builderignore: ${if;${driver;gradle};bin,bin_test,generated;build}

