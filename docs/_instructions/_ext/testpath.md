---
layout: default
class: Project
title: -testpath REPO-SPEC ( ',' REPO-SPEC ) 
summary: The specified JARs from a repository are added to the remote JVM's classpath if the JVM is started in test mode in addition to the -runpath JARs.  
---

The `-testpath` instruction specifies JARs from a repository that should be added to the remote JVM's classpath when the JVM is started in test mode. These JARs are included in addition to those specified by `-runpath` and are typically used to provide test dependencies, such as JUnit or other testing libraries.

This is useful for ensuring that all necessary test dependencies are available during test execution without including them in the main runtime classpath.
