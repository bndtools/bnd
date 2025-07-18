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

# -require-bnd

The `-require-bnd` instruction allows you to specify a filter that must match the Bnd version for the build to proceed. If the filter does not match, bnd will generate an error and stop the build. This is useful for enforcing minimum or specific Bnd versions in your build environment.

Example:

```
-require-bnd: version>=7.0.0
```

If not set, any Bnd version will be accepted.

Each specified filter must evaluate to true for the running version of Bnd in the `version` attribute. Since the values of the instruction are filter expressions, they need to quoted so the filter operators are not processed by Bnd.

This instruction can be useful when the workspace requires a feature of Bnd introduced in some version of Bnd. For example:

    -require-bnd: "(version>=4.3.0)"


<hr />
TODO Needs review - AI Generated content
