---
layout: default
class: Project
title: -diffpackages SELECTORS
summary: The names of exported packages to baseline.
---

You can use the `-diffpackages` instruction to specify the names of exported packages
to be baseline compared. The default is all exported packages.

## Attributes

The following attributes can be specified on each package selector:

### threshold

Specifies the minimum change level that should be reported. Valid values are `MICRO`, `MINOR`, or `MAJOR`. Changes below this threshold will be ignored during baselining.

Example:

    -diffpackages: *;threshold=MAJOR

This will only report MAJOR changes and ignore MINOR and MICRO changes.

### includezeromajor

By default, packages with major version `0` (i.e., `0.x.x`) do not generate baseline errors, as per [semantic versioning spec](https://semver.org/#spec-item-4) which states that major version zero is for initial development. Setting `includezeromajor=true` enables baseline error reporting for packages in the version range `[0.1.0, 1.0.0)`.

Note: Packages with version `0.0.x` are still excluded from baselining even when `includezeromajor` is enabled.

Example:

    -diffpackages: *;includezeromajor=true

## Examples

Exclude internal packages from baselining:

    -diffpackages: !*.internal.*, *

Enable baselining for 0.x versions:

    -diffpackages: *;includezeromajor=true

Set threshold for changes and enable 0.x baselining:

    -diffpackages: *;threshold=MINOR;includezeromajor=true

Combine exclusions with attributes (note: attributes only apply to their selector):

    -diffpackages: !*.internal.*, *;threshold=MAJOR;includezeromajor=true
