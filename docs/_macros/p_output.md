---
layout: default
class: 	Project
title:  p_output
summary: Get the absolute path to the project's output directory
---

## Summary

The `p_output` macro returns the absolute path to the project's output/target directory where compiled classes and built artifacts are placed.

## Syntax

```
${p_output}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns absolute path to output directory
- Typically `bin` or `target` directory
- No arguments allowed

## Examples

Get output directory:
```
Output-Directory: ${p_output}
```

Reference output in paths:
```
classes.location=${p_output}/classes
```

## Use Cases

- Referencing build output location
- Configuring post-build tasks
- Documentation of build structure
- Tool integration

## Notes

- Returns absolute path
- Usually `bin` or `target` directory
- Cannot take arguments
- See also: `${p_sourcepath}` for source directories
- See also: `${basedir}` for project root



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
