---
layout: default
class: 	Project
title: 	p_allsourcepath
summary: Get paths to all source directories
---

## Summary

The `p_allsourcepath` macro returns all source directories for the project, including those from dependencies, as a comma-separated list.

## Syntax

```
${p_allsourcepath}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns all source directories
- Includes project and dependency sources
- Paths are absolute
- Results are comma-separated

## Examples

Get all source paths:
```
All-Sources: ${p_allsourcepath}
```

## Use Cases

- Complete source documentation
- IDE integration
- Source analysis tools
- Documentation generation

## Notes

- More comprehensive than `${p_sourcepath}`
- Includes dependency sources
- Returns absolute paths
- See also: `${p_sourcepath}` for project sources only



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
