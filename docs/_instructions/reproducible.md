---
layout: default
class: Builder
title: -reproducible BOOLEAN | TIMESTAMP
summary: Ensure the bundle can be built in a reproducible manner.
---

To ensure the bundle can be built in a reproducible manner, the timestamp of the zip entries is set to the fixed time `1980-02-01T00:00:00Z` when the value of this instruction is `true`.
The value can also be set to either an ISO-8601 formatted time or the number of seconds since the epoch which is used as the timestamp for the zip entries.
The `Bnd-LastModified` header is also omitted from the manifest.
The default value is `false`.

For example:

    -reproducible: true
    -reproducible: 1641127394
    -reproducible: 2022-01-02T12:43:14Z
