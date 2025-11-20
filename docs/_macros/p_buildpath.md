---
layout: default
class: 	Project
title: 	p_buildpath
summary: Get the project's buildpath as a list
---

## Summary

The `p_buildpath` macro returns the project's buildpath (compile-time dependencies) as a comma-separated list of file paths.

## Syntax

```
${p_buildpath}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns all JARs and directories on the buildpath
- Paths are absolute
- Results are comma-separated
- Includes all `-buildpath` entries

## Examples

Get buildpath:
```
Build-Dependencies: ${p_buildpath}
```

Count buildpath entries:
```
dependency.count=${size;${split;,;${p_buildpath}}}
```

## Use Cases

- Listing compile-time dependencies
- Build documentation
- Dependency analysis
- Creating custom classpaths
- Build metadata

## Notes

- Returns absolute paths
- Includes all `-buildpath` entries
- See also: `${p_testpath}` for test dependencies
- See also: `${p_dependson}` for project dependencies



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
