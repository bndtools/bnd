# bnd-plugins-parent
=======================

This project builds the various maven plugins provided by the bnd project,
and defines common dependency information.

The plugins are built using maven (unlike the rest of bnd) because it is
very hard to build a maven plugin unless you use maven to do it!


# Plugin Projects
===================

## bnd-maven-plugin

The core bnd plugin, used to generate manifest and other metadata for 
projects that build an OSGi bundle.

## bnd-indexer-maven-plugin

A plugin used to generate an OSGi repository index from a set of maven
dependencies. The entries in the index will reference the location of
the bundles in the remote repositories to which they have been deployed.
