---
layout: default
class: 	Project
title: 	p_testpath
summary: Get the project's test runtime path
---

## Summary

The `p_testpath` macro returns the test runtime path (JARs placed on the classpath for testing) as a comma-separated list of file paths.

## Syntax

```
${p_testpath}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns all JARs on the test runtime path
- Paths are absolute
- Results are comma-separated
- Includes all `-runpath` entries for testing

## Examples

Get test path:
```
Test-Dependencies: ${p_testpath}
```

## Use Cases

- Listing test runtime dependencies
- Test configuration
- Dependency documentation
- Build metadata

## Notes

- Returns absolute paths
- Includes `-runpath` entries
- Used for test execution
- See also: `${p_buildpath}` for compile dependencies




---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
