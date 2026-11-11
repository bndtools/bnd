---
layout: default
class: Launcher
title: -runproperties PROPERTIES 
summary:  Define system properties for the remote VM.
---

The `-runproperties` instruction allows you to define system properties that will be set for the remote Java Virtual Machine (JVM) when your application is launched. These properties are passed as `-Dkey=value` arguments and can be used to configure the runtime environment, control application behavior, or provide configuration values.

This is useful for setting environment-specific options or overriding default values without modifying your application's code.

The properties `osgi.os`, `osgi.ws` and `osgi.arch` also define the platform that a resolve and the Eclipse features on the [-runrequires] and the container paths target.

[-runrequires]: runrequires.html


<hr />
TODO Needs review - AI Generated content