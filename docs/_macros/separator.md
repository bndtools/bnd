---
layout: default
class: Macro
title: separator
summary: Get the operating system's file separator character
---

## Summary

The `separator` macro returns the operating system's file separator character used in file paths.

## Syntax

```
${separator}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns `/` (forward slash) on Unix/Linux/Mac systems
- Returns `\` (backslash) on Windows systems
- Returns the value of `File.separator` in Java

## Examples

Get the file separator:
```
${separator}
# Returns: "/" on Unix, "\" on Windows
```

Build paths manually:
```
path=${basedir}${separator}src${separator}main${separator}java
```

Use in conditionals:
```
${if;${equals;${separator};/};unix;windows}
```

Replace separators:
```
${replacestring;${path};${separator};/}
```

## Use Cases

- Determining the platform at build time
- Manual path construction
- Path manipulation and normalization
- Platform-specific logic
- Cross-platform path handling

## Notes

- Returns a single character: `/` or `\`
- This is the file/directory separator, not the path separator
- Path separator (for classpath) is different - see `${pathseparator}`
- See also: `${osfile}` for OS-specific path creation
- See also: `${pathseparator}` for path list separator (`:` or `;`)




---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
