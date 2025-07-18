---
layout: default
title: -nobundles  BOOLEAN
class: Project
summary: |
   Do not build the project.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-nobundles=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/nobundles.md --><br /><br />

# -nobundles

The `-nobundles` instruction tells bnd to skip building the project. This can be useful for projects that only provide resources or configuration, or for disabling builds in certain environments.

Example:

```
-nobundles: true
```

When this instruction is set to `true`, bnd will not produce any output JARs for the project.

<hr />
TODO Needs review - AI Generated content
