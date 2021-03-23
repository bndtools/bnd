---
layout: default
class: Builder
title: -reproducible BOOLEAN
summary: Ensure the bundle can be built in a reproducible manner.
---

To ensure the bundle can be built in a reproducible manner, the timestamp of the zip entries is set to the fixed time `1980-02-01 00:00Z`. The `Bnd-LastModified` header is also omitted from the manifest. The default value is `false`.

For example:

    -reproducible: true
