---
layout: default
title: -runkeep true | false
class: Project
summary: |
   Decides to keep the framework storage directory between launching
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runkeep: true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/runkeep.md --><br /><br />

# -runkeep

The `-runkeep` instruction decides whether to keep the framework storage directory between launches. When set to `true`, the storage directory is preserved, which can be useful for debugging or maintaining state between runs.

The `-runkeep` instruction is particularly useful in scenarios where you need to persist data across different runs of the framework. For instance, if your application generates logs, caches data, or requires a specific directory structure to function correctly, setting `-runkeep` to `true` will ensure that these elements are not removed between launches.

However, one should be cautious while using this option, as preserving the storage directory might lead to outdated or corrupted data being used in subsequent runs. It is generally recommended to use the default setting (i.e., `-runkeep: false`) unless there is a specific need to retain the storage directory.

Example:

```
-runkeep: true
```

By default, the storage directory is deleted between launches.


TODO Needs review - AI Generated content
