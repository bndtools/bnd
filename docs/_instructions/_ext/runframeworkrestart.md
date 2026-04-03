---
layout: default
class: Project
title: -runframeworkrestart BOOLEAN
summary: Restart the framework in the same VM if the framework is stopped or updated. 
---

Any bundle can stop the framework. After stopping, the launcher receives a notification and normally
exit the process with `System.exit`. In some cases, usually testing, it is necessary to do a restart
in the local VM. 

If `-runframeworkrestart` is set to `true`, the launcher will not do a hard exit after the framework is stopped,
but will restart the framework after doing the normal clean up.

The launcher keeps a system property `launcher.framework.restart.count` that is set to the iteration, it is initially zero.