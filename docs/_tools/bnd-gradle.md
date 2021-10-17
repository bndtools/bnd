---
title: Bnd Gradle Plugins
layout: default
summary: The Bnd Gradle Plugins from the Bnd developers.
---

For out-of-the box workflow, we recommend starting with [Eclipse Workspace](https://bndtools.org/)

However, Eclipse is not a requirement.

When Eclipse is not an option, or you need to do CI work,
the [Gradle Plugin][1] covers everything you need.

There is also an accompanying [OSGi Starter](/assets/osgi-starter-workspace.pdf)
that provides a lot of detail how to use bnd for larger projects.
It includes a Gradle plugin out of the box that will build the Bndtools workspace
identical to how things are build inside Eclipse for Continuous Integration with Github Actions or other build servers.

See the [documentation on GitHub][1] for details on how to configure and
use the Bnd Gradle plugins.

**If you're just getting started with OSGi and bnd, we recommend [starting at the introduction](/chapters/110-introduction.html)**

If you just want to figure out how different commands and macros work,
the [bnd commandline](https://bnd.bndtools.org/chapters/400-commands.html) is your friend.
Especially the <code>shell</code> command is very useful exploring it.

[1]: https://github.com/bndtools/bnd/blob/master/gradle-plugins/README.md

