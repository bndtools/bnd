---
layout: default
class: Builder
title: -nodefaultversion  BOOLEAN
summary:  Do not add a default version to exported packages when no version is present. 
---

The `-nodefaultversion` instruction controls whether a default version is added to exported packages when no version is specified. By default, bnd will add the bundle version as the version for any exported package that does not have an explicit version. When this instruction is set to `true`, no default version will be added, and exported packages without a version will remain unversioned.

This can be useful if you want to avoid implicit versioning and ensure that only explicitly specified versions are used in your exported packages.
