---
layout: default
title: -reproducible BOOLEAN | TIMESTAMP
class: Builder
summary: |
   Ensure the bundle can be built in a reproducible manner.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-reproducible=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/reproducible.md --><br /><br />

To ensure the bundle can be built in a reproducible manner, the timestamp of the zip entries is set to the fixed time `1980-02-01T00:00:00Z` when the value of this instruction is `true`.
The value can also be set to either an ISO-8601 formatted time or the number of seconds since the epoch which is used as the timestamp for the zip entries.
The `Bnd-LastModified` header is also omitted from the manifest.
The default value is `false`.

For example:

    -reproducible: true
    -reproducible: 1641127394
    -reproducible: 2022-01-02T12:43:14Z
