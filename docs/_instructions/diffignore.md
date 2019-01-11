---
layout: default
class: Builder
title: -diffignore PACKAGE-SPEC ( ',' PACKAGE-SPEC ) *
summary: Items to ignore during baseline comparison.
---

You can use the `-diffignore` instruction to specify a comma
separated list of things to ignore during baseline comparison
such as manifest headers or entry paths.

## Example

    -diffignore: com/foo/xyz.properties, Some-Manifest-Header
