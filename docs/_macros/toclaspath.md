---
layout: default
class: Macro
title: toclasspath ';' LIST ( ';' BOOLEAN )?
summary: Convert class names to file paths
---

## Summary

The `toclasspath` macro converts fully qualified class names to file paths by replacing dots with path separators and optionally adding `.class` extension.

## Syntax

```
${toclasspath;<class-names>[;<add-extension>]}
```

## Parameters

- `class-names` - Comma-separated list of fully qualified class names
- `add-extension` (optional) - Boolean to add `.class` extension (default: true)

## Behavior

- Replaces dots (`.`) with path separators (`/`)
- Optionally adds `.class` extension (default: yes)
- Returns comma-separated list of paths

## Examples

Convert class names with extension:
```
${toclasspath;com.example.Main,com.example.Util}
# Returns: "com/example/Main.class,com/example/Util.class"
```

Convert without extension:
```
${toclasspath;org.test.TestCase;false}
# Returns: "org/test/TestCase"
```

Create paths for lookup:
```
paths=${toclasspath;${classes};true}
```

Package paths without extension:
```
${toclasspath;com.example.api;false}
# Returns: "com/example/api"
```

## Use Cases

- Converting class names to resource paths
- Building file paths from class names
- Creating include/exclude patterns
- Resource lookup paths
- Build script generation
- File system operations on classes

## Notes

- Always uses forward slash `/` as separator
- Extension parameter defaults to true
- Inverse operation of `${toclassname}`
- Useful for locating class files
- See also: `${toclassname}` for reverse operation



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
