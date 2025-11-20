---
layout: default
class: Macro
title: isfile (';' FILE )+
summary: Check if all specified paths are regular files
---

## Summary

The `isfile` macro checks whether all specified file paths are regular files (not directories or special files). It returns "true" only if all paths exist and are regular files; otherwise, it returns "false".

## Syntax

```
${isfile;<path>[;<path>...]}
```

## Parameters

- `path` - One or more file paths to check (can be absolute or relative)

## Behavior

- Converts each path to an absolute path
- Checks if each path exists and is a regular file
- Returns "true" only if ALL paths are regular files
- Returns "false" if any path is not a file, is a directory, or doesn't exist
- Issues a warning if no paths are provided

## Examples

Check a single file:
```
${isfile;build.gradle}
# Returns: "true" if build.gradle exists and is a file
```

Check multiple files:
```
${isfile;pom.xml;build.gradle;settings.gradle}
# Returns: "true" only if all three are files
```

Use in conditional logic:
```
${if;${isfile;custom.bnd};${cat;custom.bnd};default-settings}
```

Validate required files:
```
${if;${isfile;LICENSE;README.md};files-present;${error;Missing required files}}
```

Check file existence before processing:
```
config=${if;${isfile;config.properties};${cat;config.properties};{}}
```

## Use Cases

- Validating that required files exist
- Conditional logic based on file presence
- Build prerequisite checking
- File existence verification before reading
- Distinguishing between files and directories
- Checking multiple file dependencies

## Notes

- Returns "false" (not error) if paths don't exist
- All paths must be regular files for "true" result
- Returns string "true" or "false", not boolean
- Paths are resolved to absolute paths before checking
- Directories, symbolic links, and special files return "false"
- See also: `${isdir}` for checking if paths are directories
- Useful with `${if}` macro for conditional behavior
- Generates a warning if called without arguments


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
