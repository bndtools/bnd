---
title: Warnings
layout: default
---

## Java source version inconsistency: bnd has '1.5' while Eclipse has '1.8'. Set the bnd 'javac.source' property or change the Eclipse project setup.

First to explain the message. In a Bndtools project, the Java version is configured in two places. For the offline bnd build with Gradle/Ant, we set it directly from the `javac.source` and `javac.target` properties that you find in `cnf/build.bnd`. But in the internal Eclipse build, the configuration of the Eclipse project contains the JRE settings. Unfortunately we cannot force Eclipse to use the same settings as the offline build, but we can detect when they are inconsistent, and so we generate the warning you have seen.

To fix the problem, first decide what version of Java you want to use. Then you need to set that in both the bnd configuration and in the Eclipse workspace.

For bnd, you can set `javac.source` and `javac.target` for the whole workspace in `cnf/build.bnd`, and you can override it if you wish in each project's `bnd.bnd` file. You can find these entries in `cnf/build.bnd` from the standard templates, it will be commented out so just uncomment it.

For Eclipse you can set a workspace-wide preference by opening `Window -> Preferences` and navigating to the `Java -> Compiler` preference panel. This can also be overridden on a per-project basis by right-clicking the project, selecting Properties and navigating to the Java Compiler property panel. To be really safe, it's best to set the Eclipse compiler properties explicitly per-project. This is because these properties are persisted by Eclipse into the .settings folder inside the project, which can be checked into source control, whereas the workspace-wide settings go into the workspace .metadata directory which is not usually checked in.
