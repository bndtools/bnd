---
layout: default
class: Project
title: -runbundles* REPO-ENTRY ( ',' REPO-ENTRY )* 
summary:  Add additional bundles, specified with their bsn and version like in -buildpath, that are installed and started before the project is run.
---

The runbundles instruction is used to specify which bundles should be installed when a framework is started. This is the primary mechanism to run applications directly from bnd/bndtools. A bundle listed in -runbundles can be either a workspace bundle (a bundle created by one of the workspace's projects) or a bundle from one of the configured repositories. Note that all required bundles to run the application should be listed, transitive dependencies are not handles automatically so that there is full control over the runtime.

This list can be maintained manually it is normally calculated by the [resolver][1]. That is, when a resolve is run then it will, without warning, override this list.

For example:

	-runbundles: \
		org.apache.felix.configadmin;version='[1.8.8,1.8.9)',\
		org.apache.felix.http.jetty;version='[3.2.0,3.2.1)',\
		org.apache.felix.http.servlet-api;version='[1.1.2,1.1.3)',\
		...
		osgi.enroute.twitter.bootstrap.webresource;version='[3.3.5,3.3.6)',\
		osgi.enroute.web.simple.provider;version='[2.1.0,2.1.1)'

## Start Levels

Some launchers support startlevels and the `-runbundles` instruction therefore has a `startlevel` attribute. This attribute
must be a positive integer larger than 0.

    -runbundles: \
		org.apache.felix.configadmin;version='[1.8.8,1.8.9)'; startlevel=100,\
		org.apache.felix.http.jetty;version='[3.2.0,3.2.1)'; startlevel=110,\
        ...

Since the common workflow is to use the [resolver][1] to calculate the set of run bundles, any start level settings
would be overridden after the next resolve. There are the following solutions.

Use the [-runstartlevel][2] instruction to let the resolver calculate the start level ordering. In that case the
resolver will add the `startlevel` attribute.

Use the _decoration_ facility. With the decoration facility you can augment the `-runbundles` instruction by
specifying the `-runbundles+` property. The keys are _glob_ expressions and any attributes or directives
will be set (or overridden) on the merged `-runbundles` instruction.

	-runbundles: \
		org.apache.felix.configadmin;version='[1.8.8,1.8.9)',\
		org.apache.felix.http.jetty;version='[3.2.0,3.2.1)',\
		org.apache.felix.http.servlet-api;version='[1.1.2,1.1.3)',\

    -runbundles+: \
        org.apache.felix.servlet-api;startlevel=100, \
        *;startlevel=110

[1]: /chapters/250-resolving.html