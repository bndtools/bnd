---
layout: default
class: 	Project
title: p_sourcepath
summary: Get the project's source directories
---

## Summary

The `p_sourcepath` macro returns the project's source directories as a comma-separated list of paths.

## Syntax

```
${p_sourcepath}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns all source directories
- Paths are absolute
- Results are comma-separated
- Typically includes `src` directory

## Examples

Get source paths:
```
Source-Directories: ${p_sourcepath}
```

## Use Cases

- Documenting source locations
- Build configuration
- Source analysis tools
- IDE integration

## Notes

- Returns absolute paths
- Usually includes standard `src` directory
- See also: `${p_buildpath}` for dependencies
- See also: `${p_output}` for output directory




---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
