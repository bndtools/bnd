---
layout: default
class: Project
title: -runstorage FILE
summary:  Define the directory to use for the framework's work area.
---

# -runstorage

The `-runstorage` instruction defines the directory to use for the framework's work area. This is where the OSGi framework stores its runtime data, such as bundle caches and configuration.

Example:

```
-runstorage: fwstorage
```

By default, this directory is set to `fw` under the project's target directory, but you can override it with this instruction.

A framework owns its storage directory while it runs. For that time the launcher holds the lock file `<directory>.lock` next to the storage directory, and also next to the directory that `org.osgi.framework.storage` selects when that property overrides the storage. A launch that cannot take the lock, because another launch holds it or the lock file cannot be opened, fails with an error that names the reason. A second launch with the same storage directory fails with an error that names the process holding the lock, instead of deleting (see [-runkeep](/instructions/runkeep.html)) or sharing a storage that is in use. Launchers of earlier bnd versions do not take this lock.


<hr />
TODO Needs review - AI Generated content