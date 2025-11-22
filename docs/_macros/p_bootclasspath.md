---
layout: default
class: 	Project
title: 	p_bootclasspath
summary: Get the project's boot classpath
---

## Summary

The `p_bootclasspath` macro returns the project's boot classpath (Java runtime libraries) as a comma-separated list of paths.

## Syntax

```
${p_bootclasspath}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns boot classpath entries
- Typically JRE/JDK libraries
- Paths are absolute
- Results are comma-separated

## Examples

Get boot classpath:
```
Boot-Classpath: ${p_bootclasspath}
```

## Use Cases

- Java runtime configuration
- Compiler configuration
- Cross-compilation setup
- Build documentation

## Notes

- Returns JRE/JDK library paths
- Usually includes `rt.jar` or modules
- Platform-specific
- See also: `${p_buildpath}` for project dependencies




---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
