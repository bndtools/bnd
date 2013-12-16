---
title: Bndtools Concepts
description: Introduction to the Basics
author: Neil Bartlett
---

% Bndtools Concepts
% Neil Bartlett
% 13 December 2013

Repositories
============

Bndtools uses *repositories* to supply dependencies to be used at build-time and at runtime. See the [separate section on repositories](repositories.html).

Plug-ins and Plug-in Paths
==========================

Repositories and other kinds of plug-in can be added to the workspace by editing the `-plugin` entry in `cnf/build.bnd`. Plug-ins are specified by their class name, with additional configuration properties passed as follows:

    -plugin: <classname>;<property>=<value>;<property>=<value>,\
             <classname>;<property>=<value>;<property>=<value>

For example:

    -plugin: org.example.Plugin1; \
                    name=First; \
                    location=http://www.example.com/, \
             org.example.Plugin2; \
                    name=Second; \
                    licence=ApacheV2

Note that if a property value contains a comma then the whole value must be enclosed in single or double quotes, to prevent the comma being interpreted as a delimiter in the plug-in list.

If the plug-in class is not built into bnd/Bndtools then we must use the `path:` directive to specify the classpath for the plug-in, e.g.:

    -plugin: org.example.MyPlugin; \
                path:="plugins/myplugin-1.0.jar,/usr/lib/foo.jar"

Bundles and Bnd Files
=====================

Bndtools follows the "generated manifest" approach to building OSGi bundles; this is in contrast to the "manifest first" approach used by some other tools. See the OSGi Community Wiki page on [Tooling Approaches](http://wiki.osgi.org/wiki/Tooling_Approaches) for an explanation of the difference between these approaches.

Bndtools projects are based on standard Eclipse Java projects. After the Java code is compiled into classfiles, the Bndtools builder takes over and produces one or more OSGi bundles. It uses `.bnd` files to control the contents of the OSGi bundles that it generates.

The Project Settings File: bnd.bnd
----------------------------------

A Bndtools project always contains a file named `bnd.bnd`. This file contains project settings such as the Build Path, which defines the libraries that are visible to the project at build time:

![](/images/concepts/bundles01.png)

Single-Bundle and Multiple-Bundle Projects
------------------------------------------

Bndtools projects either generate a single bundle or multiple bundles. In the single-bundle mode (which is the default) all the instructions defining that bundle are listed in the `bnd.bnd` file: in other words, the `bnd.bnd` file controls both the project settings *and* the instructions for generating a single bundle.

In the multiple-bundle mode, the `bnd.bnd` still contains project settings such as Build Path. However the instructions controlling the output of bundles is separated into several other `.bnd` files. These are known as "sub-bundles", and turning on multiple-bundles mode is known as enabling "sub-bundles":

![](/images/concepts/bundles02.png)

When sub-bundles mode is turned on, one bundle is generated for *each* other `.bnd` file in the top-level of the Bndtools project. Those `.bnd` files contain only the instructions required to define the contents of a bundle such as Private Packages, Export Packages, etc.

Derived Bundle Symbolic Names
-----------------------------

The bundle symbolic name (BSN) of a generated bundle is derived from the project name and -- if sub-bundles is enabled -- the name of the sub-bundle file.

In single-bundle mode, the BSN of the bundle will be equal to the name of the Bndtools project. So if the project name is `org.example` then the BSN of the bundle will be `org.example`:

	Bundle-SymbolicName: org.example

In sub-bundles mode, the BSN of each bundle is equal the name of the project plus the name of the sub-bundle file excluding its `.bnd` extension. So if the project name is `org.example` and there is a sub-bundle file named `foo.bnd` the BSN will be `org.example.foo`:

	Bundle-SymbolicName: org.example.foo

You should not attempt to override the generated BSN, e.g. by including a `Bundle-SymbolicName` head in the `.bnd` file, because Bndtools uses the association between project names and bundle names in order to find bundles when projects depend on them.

