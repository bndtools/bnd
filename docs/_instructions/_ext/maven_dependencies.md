---
layout: default
class: Project
title: -maven-dependencies* entry ( ',' entry )*
summary:  Configure maven dependency information for the generated pom
---

The [-pom] instruction can be used to generate a pom in the bundle. The value of the `-maven-dependencies` instruction is used to generate the `<dependencies>` section in the generated pom.

## Syntax

    -maven-dependencies ::= entry ( ',' entry ) *
     entry     ::= key ( ';' attribute ) *
     attribute ::= groupId-attr | artifactId-attr | version-attr
                   | classifier-attr
     groupId-attr    ::= 'groupId' '=' groupId
     artifactId-attr ::= 'artifactId' '=' artifactId
     version-attr    ::= 'version' '=' version
     classifier-attr ::= 'classifier' '=' classifier
     gavkey    ::= groupId ':' artifactId ':' version ( ':jar:' classifier)?

## Description

For Bnd Workspace builds, the `-maven-dependencies` instruction is automatically set, if not already set, by Bnd from the [-buildpath] entries of the project. Normally, you can allow Bnd to automatically set the `-maven-dependencies` instruction. But you can override the maven dependency information by explicitly setting the `-maven-dependencies` instruction. If set to the empty string, then no `<dependencies>` section will be added in the generated pom. Since the `-maven-dependencies` instruction is a [merged instruction], you can use suffixes to override the generated information for a dependency and to add additional dependencies to the maven dependency information.

When Bnd automatically sets the `-maven-dependencies` instruction, it will generate the `key` value using the `gavkey` production with the groupId, artifactId, version, and classifier (if set) values of the artifact. Using a well defined `gavkey` value, allows the attributes of a specific artifact to be overriden by merged property value while allowing the other automatically set values to still be used. However any unique `key` value can be used if you don't care to override the automatically set value for a specific dependency.

When an artifact come from a maven repository, such as [MavenBndRepository] or [BndPomRepository], the maven repository will supply the `groupId`, `artifactId`, `version`, and `classifier` (if the artifact has a non-empty classifier) attributes of the artifact. If the artifact does not come from a maven repository but does contain a [pom.properties] resource, then the `groupId`, `artifactId`, and `version` attributes are supplied by that resource.

## Buildpath Entries

An attribute value can also be specified on a [-buildpath] entry, by prefixing the attribute name with `maven-`, which can then replace any such attribute value coming from the maven repository or a [pom.properties] resource.

You can set the `maven-scope` attribute on a [-buildpath] entry to specify a different [dependency scope] than the default dependency scope specified by the [-maven-scope] instruction. The value of the `maven-scope` attribute must be a valid dependency scope.

You can also set the `maven-optional` attribute on a [-buildpath] entry to `true` which will exclude the artifact from the generated dependencies information. The default value for `maven-optional` is `false`.

## Examples

Disable generating the `<dependencies>` section in the generated pom.

    -maven-dependencies:

Override the automatically set dependency information for a specific artifact. You must use the matching `gavkey` of the actual artifact and then specify the `groupId`, `artifactId`, and `version` attributes to use for the artitact. This example replaces the version of the artifact to remove `-SNAPSHOT`.

    -maven-dependencies.fix: org.osgi:osgi.annotation:8.0.0-SNAPSHOT;\
     groupId=org.osgi;artifactId=osgi.annotation;version=8.0.0

This could also be done on the [-buildpath] entry for the artifact by specifying the `maven-version` attribute to override the maven version of the artifact.

    -buildpath: osgi.annotation;version=8.0.0.SNAPSHOT;maven-version=8.0.0

Add an additional dependency. You must specify the `groupId`, `artifactId`, and `version` attributes to use for the artitact.

    -maven-dependencies.log: log;groupId=org.osgi;\
     artifactId=org.osgi.service.log;version=1.5.0

Don't generate a `<dependency>` element for a specific artifact. This is done by specifying the proper `gavkey` value and not specifying any attributes.

    -maven-dependencies.nodependency: org.osgi:osgi.annotation:8.0.0

This could also be done on the [-buildpath] entry for the artifact by specifying the `maven-optional` attribute with the value `true`.

    -buildpath: osgi.annotation;version=8.0.0;maven-optional=true

Change the dependency scope for the artifact. The default dependency scope is specified by the [-maven-scope] instruction.

    -buildpath: osgi.annotation;version=8.0.0;maven-scope=provided

[-pom]: pom.html
[-buildpath]: buildpath.html
[-maven-scope]: maven_scope.html
[MavenBndRepository]: ../plugins/maven.html
[BndPomRepository]: ../plugins/pomrepo.html
[merged instruction]: ../chapters/820-instructions.html#merged-instructions
[pom.properties]: https://maven.apache.org/shared/maven-archiver/#pom-properties-content
[dependency scope]: https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Scope
