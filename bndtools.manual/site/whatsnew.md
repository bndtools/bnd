---
title: New in Bndtools 2.1
description: Based on bnd
author: Ferry Huberts
---


Introduction
============
This new version has many improvements that - not only - make it faster again,
but also expand the functionality to offer a more complete experience.

We've focused on bug fixes with an increased attention to details and also
added some new functionality.




Important Notes
===============
* We're deprecating running from `*.bnd` files, which is why we're no longer
showing the ***Run Requirement/Resolve*** panel on the Run tab. Users are
**strongly advised** to migrate their run settings into `*.bndrun` files. Expect
the Run tab to be removed completely for `*.bnd` files in a future release.

* Existing workspaces should update their `cnf/buildrepo/biz.aQute.launcher`
to version 1.0.6 to fix issues with paths containing whitespace. Download
it [here][5], or generate a new `cnf` project by removing the existing one.




Program Arguments
=================
You can now specify program (command line) arguments on the Run tab.

This enables running and debugging your application in Eclipse as if you were
running it from the command line.

For more details, see [the wiki page explaining its usage][3].




Version Control Ignore Files
============================
Bndtools will now (by default) generate `.gitignore` files when you create new
Bndtools projects so that you don't commit derived files.

This can be switched of in the Bndtools preferences panel.

This feature is still under active development and expected to gain support
for other version control systems than Git.




JAR Viewer Improvements
=======================
The JAR viewer now by default opens the `META-INF/MANIFEST.MF` file when
opening a JAR file. 

It will also remember the selected file so that when the JAR is updated,
the same file is still shown after the JAR is reloaded.




Paths Containing Whitespace
===========================
Bndtools now (finally) fully supports paths containing whitespace.

This was a long standing bug that affected launching from Bndtools.
See also the [Important Notes][4] above.




Stricter Compilation
====================
A compiler error is now generated on incorrect usage of the `@Reference`
annotation.

An example:
You have a setter (e.g. set/add) for a dynamic dependency,
but no unsetter (e.g. unset/remove).




Speed Improvements
==================
We again improved the build speed by being even smarter about dependencies
than in the 2.0.0 release, and as a result the build is again faster than
before.




Miscellaneous
=============
Of course there are many more gems in the changes.
A full list of changes can be found on our [wiki][2]




Installation
============
You can install Bndtools into Eclipse through the marketplace or use our
Eclipse update site, see the [installation instructions][8] for further
instructions.

For those of you that want to live on the (bleeding) edge, our latest stable
build lives [here][6].

As usual, please report any issues you find on our [Github bug tracker][7].





Older Releases
==============
* Version [2.0.0.REL][1], released on February 11, 2013




[1]: ./whatsnew2-0-0.html
[2]: https://github.com/bndtools/bndtools/wiki/Changes-in-2.1.0
[3]: https://github.com/bndtools/bndtools/wiki/Program-Arguments
[4]: #important-notes
[5]: https://bndtools.ci.cloudbees.com/job/bnd.master/72/artifact/dist/bundles/biz.aQute.launcher/biz.aQute.launcher-1.0.6.jar
[6]: https://bndtools.ci.cloudbees.com/job/bndtools.master/lastSuccessfulBuild/artifact/bndtools.build/generated/p2
[7]: https://github.com/bndtools/bndtools/issues
[8]: http://bndtools.org/installation.html