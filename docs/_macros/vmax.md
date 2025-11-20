---
layout: default
class: Macro
title: vmax (';' LIST )*
summary: Find the maximum version in one or more lists
---

## Summary

The `vmax` macro compares version strings using OSGi semantic versioning rules and returns the maximum (highest) version.

## Syntax

```
${vmax;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists of version strings

## Behavior

- Parses all values as OSGi versions
- Compares using semantic versioning rules
- Returns the highest version string
- Handles major.minor.micro.qualifier format

## Examples

Find maximum version:
```
${vmax;1.2.3,2.0.0,1.0.5}
# Returns: "2.0.0"
```

Multiple lists:
```
${vmax;1.5.0,2.0.0;1.2.3,3.0.0}
# Returns: "3.0.0"
```

Get latest version:
```
latest.version=${vmax;${available.versions}}
```

## Use Cases

- Finding latest version
- Version range calculations
- Dependency analysis
- Latest version selection
- Version comparison

## Notes

- Uses OSGi semantic versioning
- Qualifiers compared lexicographically
- Invalid versions may cause errors
- See also: `${vmin}` for minimum version
- See also: `${vcompare}` for comparison


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
