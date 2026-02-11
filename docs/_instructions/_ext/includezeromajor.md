---
layout: default
class: Project
title: -includezeromajor BOOLEAN
summary: Enable baselining for packages with major version 0.
---

# -includezeromajor

The `-includezeromajor` instruction enables baseline error reporting for packages with major version `0` (i.e., versions in the range `[0.1.0, 1.0.0)`).

By default, bnd does not report baselining errors for packages with major version `0`, following the [semantic versioning spec](https://semver.org/#spec-item-4) which states that "Major version zero (0.y.z) is for initial development. Anything may change at any time. The public API should not be considered stable."

When set to `true`, baseline error reporting is enabled for packages with version `0.1.0` or higher. Packages with version `0.0.x` are still excluded from baselining.

Example:

```
-baseline: *
-includezeromajor: true
```

This is useful when you want to enforce semantic versioning discipline during the initial development phase (0.x versions) while still excluding the very early `0.0.x` versions.

Default value is `false`.
