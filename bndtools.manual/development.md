---
title: Bndtools Developer Guide
description: Guide to checking out and building Bndtools from source.
author: Neil Bartlett
---

**This page is a tutorial for developers wishing to customise and enhance Bndtools itself.** For general documentation please refer to the [Tutorial][1].

Building Bndtools
=================

Bndtools is built with Bndtools! If you want to work on the bndtools source code, you have two options:

* Install the current public release of bndtools from the [Eclipse Marketplace][2] and start working straight away.
* Build Bndtools from the command line using bnd (with ANT), then install the build results into your Eclipse IDE.

Checking Out from GitHub
========================

First check out the source code from GitHub as follows:

	git clone git://github.com/bndtools/bndtools.git

If you have Bndtools installed in your Eclipse IDE already (e.g. using Marketplace) then skip to **Importing Into Eclipse** below. Otherwise read on...

Building With bnd/ANT
=====================

Assuming you have Apache ANT (version 1.7 or better) installed, you can build bndtools from the command line by changing to the `bndtools.build` directory and typing:

	ant build p2

After a minute or two, a directory named `generated/p2` will appear. This contains an Eclipse Update Site that you can use to install bndtools from the code you have just built.

To install from the generated Update Site, open the Help menu in Eclipse and select "Install New Software". In the update dialog, click the "Add" button (near the top left) and then click the "Local" button. Browse to the location of the `bndtools.build/generated/p2` directory that you just built. Then set the name of this update site to "Bndtools Local Snapshot" (or whatever you like, it's not really important so long as you enter *something*). Click "OK".

Back in the update dialog, Bndtools will appear in the category list. Place a check next to it and click Next. Drive the rest of the wizard to completion... congratulations, you have just built and installed bndtools!

Importing Into Eclipse
======================

Now you have Bndtools installed in your Eclipse IDE, you can import the bndtools projects into Eclipse to begin working on the source code.

Open the File menu and select "Import" and then "Existing Projects into Workspace" (under the General category). Click "Next". Click the "Browse" button (top right) and select the root directory of the bndtools projects.

Ensure that all the following projects are checked:

* `cnf`
* `bndtools.bndplugins`
* `bndtools.core`
* `bndtools.jareditor`
* `bndtools.build`
* `bndtools.manual`
* `bndtools.release`
* `bndtools.repository.base`

NB: These projects must all be in the same directory!

Click "Finish"... Eclipse will start to import and build the projects. **If you see a dialog during the import prompting you to "Create a Bnd Configuration Project" click CANCEL.**

You should now have all the bndtools projects in your workspace, ready to begin hacking!

Launching Bndtools from Eclipse
===============================

To launch bndtools from Eclipse (e.g. to try out a change to debug), use one of the `.bndrun` files from the `bndtools.core` project. There are three launchers, one per architecture, i.e.:

* `bndtools.cocoa.macosx.x86_64.bndrun` for running on Mac OS X (64-bit Intel x86)
* `bndtools.gtk.linux.x86.bndrun` for running on Linux (32-bit Intel x86).
* `bndtools.win32.x86.bndrun` for running on Win32 (XP, Vista etc).

Right click on the file that matches your computer's architecture and select "Run As" > "OSGi Run". If none of these files matches the architecture you want to run on, then please create a new one and submit it back as a patch.

[1]: /tutorial.html "Bndtools Tutorial"
[2]: http://marketplace.eclipse.org/ "Eclipse Marketplace"