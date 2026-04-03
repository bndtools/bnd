---
layout: default
class: Runtime
title: -resolve.excludesystem true|false
summary: A property used by the resolver, if set to true (default) it excludes the system resource
---

This property has no meaning in the normal configuration. It is used by code that needs to
have the _system resource_ in the wiring. Normally the wiring excludes the system resource.
However, sometimes it is necessary to see the full solution.