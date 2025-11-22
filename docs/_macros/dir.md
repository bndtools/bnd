---
layout: default
class: Macro
title: dir ( ';' FILE )*
summary: Extract the directory path from one or more file paths
---

## Summary

The `dir` macro extracts and returns the parent directory path from one or more file paths. It returns the absolute path to each file's containing directory, filtering out non-existent files.

## Syntax

```
${dir;<filepath>[;<filepath>...]}
```

## Parameters

- `filepath` - One or more file paths (relative to project directory)

## Behavior

- Resolves each file path relative to the project directory
- Filters to include only files that exist
- Returns the absolute path to each file's parent directory
- Multiple directory paths are concatenated with commas
- Returns null if no file arguments are provided
- Issues a warning if called without arguments

## Examples

Get directory of a single file:
```
source.dir=${dir;src/main/java/Example.java}
# Returns: /path/to/project/src/main/java
```

Get directories of multiple files:
```
dirs=${dir;LICENSE.txt;README.md}
# Returns: /path/to/project,/path/to/project
```

Use in path construction:
```
parent.dir=${dir;${@}}
# Gets directory containing current bundle JAR
```

Build relative path:
```
Bundle-License: ${dir;LICENSE}/LICENSE.txt
```

## Use Cases

- Determining the location of source files
- Building paths relative to file locations
- Extracting directory structure information
- Creating directory-based configurations
- Referencing sibling files in the same directory
- Computing relative paths for resources

## Notes

- File paths are resolved relative to the project base directory
- Only existing files are processed; non-existent files are silently ignored
- Returns absolute paths, not relative paths
- The result uses platform-specific path separators
- Empty result if all specified files don't exist


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
