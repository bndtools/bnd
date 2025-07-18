---
layout: default
title: -runstorage FILE
class: Project
summary: |
   Define the directory to use for the framework's work area.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runstorage= foo`

- Pattern: `.*`

<!-- Manual content from: ext/runstorage.md --><br /><br />

# -runstorage

The `-runstorage` instruction defines the directory to use for the framework's work area. This is where the OSGi framework stores its runtime data, such as bundle caches and configuration.

Example:

```
-runstorage: fwstorage
```

By default, this directory is set to `fw` under the project's target directory, but you can override it with this instruction.


<hr />
TODO Needs review - AI Generated content
