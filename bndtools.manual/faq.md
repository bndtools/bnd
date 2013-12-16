---
title: Bndtools FAQ
description: Frequently Asked Questions
author: Neil Bartlett
---

How Do I Add Bundles to the Repository?
=======================================

There are several ways to add external bundles into your repository in order to make them available in your projects. The easiest way is to drag-and-drop from your native file management application (i.e. Windows Explorer, Mac Finder, etc) into the **Repositories** view in Bndtools. Note that you need to drop it onto a specific repository entry: usually the "Local Repository". Note that you can multi-select many bundle files and drag/drop them at the same time.

![](/images/faq/01.png)

Importing the bundles this way is better than directly modifying the contents of the `cnf/repo` directory, since Bndtools is immediately aware of the new bundles and regenerates the OBR index for the repository.

**NB:** not all repository types support adding bundles! For example, the `OBR` repository type is based on an index file that may be anywhere local or remote; Bndtools would not know where to put the new bundle or how to update the index. However the following repository types *do* support additions:

* `LocalOBR`
* `FileRepo`

How Do I Remove Bundles from a Repository?
==========================================

Unfortunately the current repository API does not support a generic way to remove bundles. We plan to update this API, but for now it is necessary to remove the bundles manually from the filesystem.

For repositories of type `LocalOBR` perform the following steps:

1. Delete the bundle file from `<repo_dir>/<bsn>`. If you want to delete all versions of the bundle, then also delete the `<bsn>` directory itself (i.e., do not leave an empty directory).
2. Delete the OBR index file, `<repo_dir>/repository.xml`.
3. Touch the `cnf/build.bnd` file. This causes the index file to be regenerated and the repository contents to be refreshed.

For repositories of type `FileRepo`, perform the same steps except for deleting the OBR index (which will not exist anyway).

Why is My Bundle Empty?
=======================

In Bndtools the contents of a bundle must be defined explicitly; if the contents are not defined then the bundle will be empty. Bundle contents are defined using the following three instructions (follow links to the bnd documentation for these instructions):

* [`Private-Package`](http://www.aqute.biz/Bnd/Format#private-package) defines Java packages to be included in the bundle but not exported.
* [`Export-Package`](http://www.aqute.biz/Bnd/Format#export-package) defines Java packages to be both included in the bundle *and* exported.
* [`Include-Resource`](http://www.aqute.biz/Bnd/Format#include-resource) defines non-Java resource files (e.g. images, XML files etc) to be included in the bundle.

Note that both `Private-Package` and `Export-Package` may be edited using the *Contents* tab of the GUI editor. There is currently no GUI assistance for the `Include-Resource` instruction so it must be entered using the *Source* tab.

What's the Difference Between "-include" and "Include-Resource"?
================================================================

The [`-include`](http://www.aqute.biz/Bnd/Format#directives) instruction is used to include a set of bnd instructions from another `.bnd` file into the current `.bnd` file. This can be useful if there are common settings or instructions used in multiple places.

The [`Include-Resource`](http://www.aqute.biz/Bnd/Format#include-resource) instruction tells bnd to include a set of non-Java resources (e.g. images, XML files etc) in the output bundle.

How Can I Configure the System Bundle Exports?
==============================================

To add extra packages to the exports of the OSGi System Bundle, use the `-runsystempackages` instruction to your run configuration file. For example:

	-runsystempackages: sun.reflect

You can also add library JARs to the Java application "classpath" using the `-runpath` instruction, which would then allow you to expose the contents of those libraries via system bundle exports:

	-runpath: jide-oss-1.0.0.jar
	-runsystempackages: com.jidesoft.swing, com.jidesoft.animation

Note that using the `-runsystempackages` instruction is equivalent to setting the OSGi property `org.osgi.framework.system.packages.extra`; however it is better to use `-runsystempackages` because then the Run Requirements resolver inside Bndtools will take account of the availability of those packages during resolution.

How Can I Depend on a Plain JAR File at Build Time?
===================================================

Sometimes it is useful to add a plain JAR file to the build-time dependencies of a project. For example we may want to use a "pure" API JAR to build against, but use a different artefact at runtime. Or we may be planning to embed the dependency in our bundle.

Plain JAR files should be added to the Build Path using the `version=file` attribute. Unfortunately there is currently no GUI support for this attribute so you will have to edit the *Source* tab:

	-buildpath: libs/servlet-api-3.0.jar;version=file

Note that this approach is better than using Eclipse's *Add to Build Path* action, because the latter will not be visible when the project is built offline using ANT; i.e. a project that compiles in Eclipse may not compile in ANT. Using the `-buildpath` approach ensures that both Eclipse and ANT build the project in exactly the same way.
