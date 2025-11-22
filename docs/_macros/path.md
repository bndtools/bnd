---
layout: default
class: Macro
title: path ( ';' FILES )+
summary: Join file paths with the OS path separator
---

## Summary

The `path` macro joins one or more lists of file paths using the operating system's path separator (`:` on Unix, `;` on Windows). This is useful for creating classpaths and other path lists.

## Syntax

```
${path;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists of file paths

## Behavior

- Combines all provided lists
- Joins paths using the OS-specific path separator
  - Unix/Linux/Mac: `:` (colon)
  - Windows: `;` (semicolon)
- Returns a platform-appropriate path list string

## Examples

Create a classpath on Unix:
```
${path;lib/a.jar,lib/b.jar,lib/c.jar}
# Returns: "lib/a.jar:lib/b.jar:lib/c.jar"
```

Create a classpath on Windows:
```
${path;lib/a.jar,lib/b.jar,lib/c.jar}
# Returns: "lib/a.jar;lib/b.jar;lib/c.jar"
```

Combine multiple path lists:
```
${path;${buildpath};${testpath}}
# Joins both lists with OS separator
```

Build Java classpath:
```
-classpath=${path;${lsr;lib;*.jar}}
```

## Use Cases

- Creating Java classpaths
- Building PATH environment variables
- Generating OS-specific path lists
- Combining multiple path sources
- Creating portable path specifications
- Tool invocation with path arguments

## Notes

- Uses the OS-native path separator automatically
- Unix/Mac uses colon `:`, Windows uses semicolon `;`
- Input lists use comma or semicolon separation
- Output uses the OS path separator
- See also: `${pathseparator}` to get the separator itself
- See also: `${osfile}` for OS-specific file paths
- See also: `${join}` for comma-separated joining




---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
