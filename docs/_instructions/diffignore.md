---
layout: default
title: -diffignore SELECTORS
class: Project
summary: |
   Manifest header names and resource paths to ignore during baseline comparison.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-diffignore=Bundle-Version`

- Pattern: `.*`

<!-- Manual content from: ext/diffignore.md --><br /><br />

You can use the `-diffignore` instruction to specify manifest header names
and resource paths to ignore during baseline comparison.

## Example

    -diffignore: com/foo/xyz.properties, Some-Manifest-Header
