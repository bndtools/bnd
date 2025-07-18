---
layout: default
title: -runframework ( 'none' | 'services' | ANY )?
class: Launcher
summary: |
   Sets the type of framework to run. If 'none', an internal dummy framework is used. Otherwise the Java META-INF/services model is used for the FrameworkFactory interface name.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runfw: none`

- Values: `(none | services | ANY)`

- Pattern: `.*`

<!-- Manual content from: ext/runframework.md --><br /><br />

The `-runframework` instruction sets the type of framework to run when launching an OSGi application. If set to `none`, an internal dummy framework is used. If set to `services` or any other value, the Java META-INF/services mechanism is used to locate the `FrameworkFactory` implementation.

Note: The name of this instruction is somewhat confusing due to historical reasons. In most cases, you should use the `-runfw` instruction to specify the actual framework JAR. The `-runframework` instruction is only needed if you want to control the framework discovery mechanism or use a dummy framework for testing purposes.


TODO Needs review - AI Generated content
