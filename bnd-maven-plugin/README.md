bnd maven plugin
================
Toni Menzel created a proof of concept plugin to demonstrate that you can build bnd(tools) projects
with maven. This project has been adapted then to be closer to bnd and handle some more details. The
result is a plugin that fully builds any bnd project. It even supports multiple bundle projects, the
multiple jars are then created with classifiers.

This branch shows that this can be used to build bnd itself. To test this out, do the following:

* In bnd-maven-plugin: mvn clean install
* cd ../cnf
* mvn clean install

The intention of this is to become a full blown plugin that is supported by bndtools

Issues
======
There is a TODO in the cnf/pom.xml (a dep I do not understand)

I do not understand why I need to define the compiler plugin

I do not understand why the plugin creates a life cycle? How are multiple plugins resolved when each
defines its own life cycle.

There are several TODOs in the java source




Previous
===

pax-bnd-mavenplugin
===================

THIS IS A PROOF OF CONCEPT, NOT A FULL BLOWN PROJECT YET.

An alternative BND-first maven plugin for OSGi

It fully builds on BNDLib for collecting classpath resources (instead of maven dependencies).
This enables full transparent maven builds using resolvers provided by the BNDTOols project (OBR etc.).
The plugin completely bypasses maven dependencies to give all control to BND.

The following phases are overwritten by this plugin:
- generate_sources: builds the classpath from BND Resolvers.
- package: generates Bundle using BNDLib (just like maven bundle plugin, but natively using the resources provided by BND instead of Maven).

Example: 
- check this project: https://github.com/tonit/workspaceBNDBridgeExample

How to build:
run mvn clean install

Toni Menzel / Rebaze.com
