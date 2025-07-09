---
layout: default
title: -runframeworkrestart BOOLEAN
class: Project
summary: |
   Restart the framework in the same VM if the framework is stopped or updated.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runframeworkrestart: true`

- Pattern: `.*`

<!-- Manual content from: ext/runframeworkrestart.md --><br /><br />

Any bundle can stop the framework. After stopping, the launcher receives a notification and normally
exit the process with `System.exit`. In some cases, usually testing, it is necessary to do a restart
in the local VM. 

If `-runframeworkrestart` is set to `true`, the launcher will not do a hard exit after the framework is stopped,
but will restart the framework after doing the normal clean up.

The launcher keeps a system property `launcher.framework.restart.count` that is set to the iteration, it is initially zero.
