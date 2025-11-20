---
layout: default
class: Macro
summary: Get the operating system's path separator character
title: pathseparator
---

## Summary

The `pathseparator` macro returns the operating system's path separator character used to separate entries in path lists like classpaths.

## Syntax

```
${pathseparator}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns `:` (colon) on Unix/Linux/Mac systems
- Returns `;` (semicolon) on Windows systems
- Returns the value of `File.pathSeparator` in Java

## Examples

Get the path separator:
```
${pathseparator}
# Returns: ":" on Unix, ";" on Windows
```

Build a path manually:
```
classpath=lib/a.jar${pathseparator}lib/b.jar${pathseparator}lib/c.jar
```

Use in conditionals:
```
${if;${equals;${pathseparator};:};unix;windows}
```

Split paths:
```
${split;${pathseparator};${some.path}}
```

## Use Cases

- Determining the platform at build time
- Manual path construction
- Splitting path strings
- Platform-specific logic
- Cross-platform path handling
- Tool configuration

## Notes

- Returns a single character: `:` or `;`
- This is different from the file separator (`/` or `\`)
- Used for separating multiple paths (like in CLASSPATH)
- See also: `${separator}` for the file separator
- See also: `${path}` which automatically uses the correct separator
- The path separator is determined by the OS running the build



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
