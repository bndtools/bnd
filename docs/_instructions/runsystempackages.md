---
layout: default
class: Launcher
title: -runsystempackages* PARAMETERS 
summary:  Define extra system packages (packages exported from the remote VM -runpath).
---

The resolver will analyses the `-runpath` and `-runfw` JARs for any exported packages and make these available as system packages to the bundles. However, in certain cases this automatic analysis does not suffice. In that case extra packages can be added. These packages must, of course, be available on the class path.

For example:

	-runsystempackage: \
		com.sun.misc
