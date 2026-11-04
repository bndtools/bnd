---
layout: bnd
title: -diffpackages SELECTORS
class: Project
summary: |
   The names of exported packages to baseline.
parent: Instruction Reference
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-diffpackages=!*.internal.*, *`

- Pattern: `.*`

<!-- Manual content from: ext/diffpackages.md --><br /><br />

You can use the `-diffpackages` instruction to specify the names of exported packages
to be baseline compared. The default is all exported packages.

## Attributes

The following attributes can be specified on each package selector:

### threshold

Specifies the minimum change level that should be reported. Valid values are `MICRO`, `MINOR`, or `MAJOR`. Changes below this threshold will be ignored during baselining.

Example:

    -diffpackages: *;threshold=MAJOR

This will only report MAJOR changes and ignore MINOR and MICRO changes.

## Examples

Exclude internal packages from baselining:

    -diffpackages: !*.internal.*, *

Set threshold for changes:

    -diffpackages: *;threshold=MINOR
