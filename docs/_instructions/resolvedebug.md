---
layout: default
title: -resolvedebug INTEGER
class: Workspace
summary: |
   Display debugging information for a resolve operation
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-resolvedebug: 1`

- Values: `0,1,2,3`

- Pattern: `.*`

<!-- Manual content from: ext/resolvedebug.md --><br /><br />

When resolving, the `-resolvedebug` instruction can be used to request debug information about the resolve is displayed to `System.out`. The value `0` turns off displaying debug information. The values `1`, `2`, and `3` display progressively more debug information.

    -resolvedebug: 1
