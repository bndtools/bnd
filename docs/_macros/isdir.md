---
layout: default
class: Macro
title: isdir ( ';' FILE )+
summary: Check if all specified paths are directories
---

## Summary

The `isdir` macro checks whether all specified file paths are directories. It returns "true" only if all paths exist and are directories; otherwise, it returns "false".

## Syntax

```
${isdir;<path>[;<path>...]}
```

## Parameters

- `path` - One or more file paths to check (can be absolute or relative)

## Behavior

- Converts each path to an absolute path
- Checks if each path exists and is a directory
- Returns "true" only if ALL paths are directories
- Returns "false" if any path is not a directory or doesn't exist
- Returns "false" if no paths are provided

## Examples

Check a single directory:
```
${isdir;src/main/java}
# Returns: "true" if src/main/java is a directory
```

Check multiple directories:
```
${isdir;src;test;resources}
# Returns: "true" only if all three are directories
```

Use in conditional logic:
```
${if;${isdir;generated};directory-exists;directory-missing}
```

Conditional file processing:
```
${if;${isdir;${basedir}/config};${cat;config/settings.xml};default-config}
```

Validate project structure:
```
structure.valid=${isdir;src/main/java;src/main/resources;src/test/java}
```

## Use Cases

- Validating that required directories exist
- Conditional logic based on directory presence
- Project structure validation
- Build prerequisite checking
- Directory existence verification
- Distinguishing between files and directories

## Notes

- Returns "false" (not error) if paths don't exist
- All paths must be directories for "true" result
- Returns string "true" or "false", not boolean
- Paths are resolved to absolute paths before checking
- No distinction between non-existent and non-directory paths
- See also: `${isfile}` for checking if paths are files
- Useful with `${if}` macro for conditional behavior


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
