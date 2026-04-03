---
layout: default
class: Project
title: -maven-scope dependency-scope
summary:  Set the default Maven dependency scope to use when generating dependency information in the generated pom
---

The [-pom] instruction can be used to generate a pom in the bundle.  The `-maven-scope` instruction can be used to specify the default dependency scope to use when Bnd generates maven dependency information for a [-buildpath] entry that will be used to create a `<dependency>` in the generated pom.

Also see the [-maven_dependencies] instruction for information on how to manualy configure the maven dependency information in a generated pom.

## Default behavior

`-maven-scope` defaults to the value `compile` which is the default [dependency scope] for Maven.

## Examples

Change the default dependency scope to `provided`.

    -maven-scope: provided

[-pom]: pom.html
[-buildpath]: buildpath.html
[-maven_dependencies]: maven_dependencies.html
[dependency scope]: https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Scope
