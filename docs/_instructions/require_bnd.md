---
layout: default
title: -require-bnd  (FILTER ( ',' FILTER )* )?
class: Project
summary: |
   The filter can test against 'version', which will contain the Bnd version. If it does not match, Bnd will generate an error.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-require-bnd="(version>=4.1)"`

- Values: `(FILTER ( ',' FILTER )* )?`

- Pattern: `.*`

<!-- Manual content from: ext/require_bnd.md --><br /><br />

Each specified filter must evaluate to true for the running version of Bnd in the `version` attribute. Since the values of the instruction are filter expressions, they need to quoted so the filter operators are not processed by Bnd.

This instruction can be useful when the workspace requires a feature of Bnd introduced in some version of Bnd. For example:

    -require-bnd: "(version>=4.3.0)"
