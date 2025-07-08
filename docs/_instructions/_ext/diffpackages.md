---
layout: default
class: Project
title: -diffpackages SELECTORS
summary: The names of exported packages to baseline.
---

You can use the `-diffpackages` instruction to specify the names of exported packages
to be baseline compared. The default is all exported packages.

## Example

    -diffpackages: !*.internal.*, *
