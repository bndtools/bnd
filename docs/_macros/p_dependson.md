---
layout: default
class: 	Project
title: 	p_dependson
summary: Get list of project names this project depends on
---

## Summary

The `p_dependson` macro returns a comma-separated list of project names that the current project depends on (as specified in the `-dependson` directive).

## Syntax

```
${p_dependson}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns project names from `-dependson` directive
- Results are comma-separated
- Returns paths to dependent projects

## Examples

Get dependent projects:
```
Depends-On: ${p_dependson}
```

## Use Cases

- Documenting project dependencies
- Build order determination
- Dependency analysis
- Project relationship documentation

## Notes

- Returns project names/paths
- Based on `-dependson` directive
- Used for workspace project dependencies
- See also: `${p_buildpath}` for JAR dependencies




---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
