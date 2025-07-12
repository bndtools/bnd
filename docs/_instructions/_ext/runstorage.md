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
