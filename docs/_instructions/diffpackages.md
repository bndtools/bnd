---
layout: default
title: -diffpackages SELECTORS
class: Project
summary: |
   The names of exported packages to baseline.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-diffpackages=!*.internal.*, *`

- Pattern: `.*`

<!-- Manual content from: ext/diffpackages.md --><br /><br />

You can use the `-diffpackages` instruction to specify the names of exported packages
to be baseline compared. The default is all exported packages.

## Example

    -diffpackages: !*.internal.*, *
