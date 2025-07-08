---
layout: default
title: -runsystempackages* PARAMETERS
class: Launcher
summary: |
   Define extra system packages (packages exported from the remote VM -runpath).
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runsystempackages=com.acme.foo,javax.management`

- Pattern: `.*`

<!-- Manual content from: ext/runsystempackages.md --><br /><br />

The resolver will analyse the `-runpath` and `-runfw` JARs for any exported packages and make these available as system packages to the bundles. However, in certain cases this automatic analysis does not suffice. In that case extra packages can be added. These packages must, of course, be available on the class path.

For example:

	-runsystempackages: \
		com.sun.misc
