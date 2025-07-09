---
layout: default
title: -resolve.excludesystem true|false
class: Runtime
summary: |
   A property used by the resolver, if set to true (default) it excludes the system resource
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Values: `true|false`

- Pattern: `.*`

<!-- Manual content from: ext/resolve.excludesystem.md --><br /><br />

This property has no meaning in the normal configuration. It is used by code that needs to
have the _system resource_ in the wiring. Normally the wiring excludes the system resource.
However, sometimes it is necessary to see the full solution.
