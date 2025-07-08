---
layout: default
title: -conduit
class: Project
summary: |
   This project is a front to one or more JARs in the file system
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-conduit= jar/osgi.jar`

- Pattern: `.*`

<!-- Manual content from: ext/conduit.md --><br /><br />

The `-conduit` instruction allows a Project to act as a conduit to one or more actual JARs on the file system. That is, when the Project is build it will not build those JARs, it just returns them as the result of the project. This can be useful when a Project is moved elsewhere but must still be part of the build because, for example, it needs to be part of the release process.

	-conduit: jar/foo.jar
	
Notice that you can use the `${lsa}` macro to get the contents of a directory:

	-conduit: ${lsa;jar/*.jar}
