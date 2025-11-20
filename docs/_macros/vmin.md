---
layout: default
class: Macro
title: vmin (';' LIST )*
summary: Find the minimum version in one or more lists
---

## Summary

The `vmin` macro compares version strings using OSGi semantic versioning rules and returns the minimum (lowest) version.

## Syntax

```
${vmin;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists of version strings

## Behavior

- Parses all values as OSGi versions
- Compares using semantic versioning rules
- Returns the lowest version string
- Handles major.minor.micro.qualifier format

## Examples

Find minimum version:
```
${vmin;1.2.3,2.0.0,1.0.5}
# Returns: "1.0.5"
```

Multiple lists:
```
${vmin;1.5.0,2.0.0;1.2.3,3.0.0}
# Returns: "1.2.3"
```

Check compatibility:
```
min.required=${vmin;${bundle.versions}}
```

## Use Cases

- Finding oldest compatible version
- Version range calculations
- Dependency analysis
- Compatibility checking
- Version comparison

## Notes

- Uses OSGi semantic versioning
- Qualifiers compared lexicographically
- Invalid versions may cause errors
- See also: `${vmax}` for maximum version
- See also: `${vcompare}` for comparison


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
