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
