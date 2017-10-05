---
layout: default
class: Project
title: -runbundles* REPO-ENTRY ( ',' REPO-ENTRY )* 
summary:  Add additional bundles, specified with their bsn and version like in -buildpath, that are installed and started before the project is run.
---

The runbundles instruction is used to specify which bundles should be installed when a framework is started. This is the primary mechanism to run applications directly from bnd/bndtools. A bundle listed in -runbundles can be either a workspace bundle (a bundle created by one of the workspace's projects) or a bundle from one of the configured repositories. Note that all required bundles to run the application should be listed, transitive dependencies are not handles automatically so that there is full control over the runtime.

This list can be maintained manually it is normally calculated by the resolver. That is, when a resolve is run then it will, without warning, override this list.

For example:

	-runbundles: \
		org.apache.felix.configadmin;version='[1.8.8,1.8.9)',\
		org.apache.felix.http.jetty;version='[3.2.0,3.2.1)',\
		org.apache.felix.http.servlet-api;version='[1.1.2,1.1.3)',\
		...
		osgi.enroute.twitter.bootstrap.webresource;version='[3.3.5,3.3.6)',\
		osgi.enroute.web.simple.provider;version='[2.1.0,2.1.1)'

