---
layout: default
class: Macro
title: basename ( ';' FILEPATH ) +
summary: Extract the filename from one or more file paths
---

## Summary

The `basename` macro extracts the filename (last component) from one or more file paths. It returns only the filenames for files that exist.

## Syntax

```
${basename;<filepath>[;<filepath>...]}
```

## Parameters

- `filepath` - One or more file paths (relative to project directory)

## Behavior

- Resolves each path relative to project directory
- Filters to include only existing files
- Returns the filename (last path component) for each
- Multiple filenames are comma-separated
- Generates warning if called without arguments

## Examples

Get single filename:
```
${basename;/path/to/file.txt}
# Returns: "file.txt"
```

Multiple files:
```
${basename;src/Main.java;src/Util.java}
# Returns: "Main.java,Util.java"
```

With project-relative paths:
```
${basename;${@}}
# Returns filename of current bundle JAR
```

Extract from full paths:
```
${basename;${buildpath}}
# Returns basenames of all buildpath entries
```

## Use Cases

- Extracting filenames from full paths
- Getting JAR names from paths
- Display names in logs
- Creating filename lists
- Path manipulation
- Build output naming

## Notes

- Only processes existing files
- Returns filename only, not the directory
- Path is resolved relative to project base
- Non-existent files are silently ignored
- Empty result if no files exist
- See also: `${dir}` for directory path
- See also: `${stem}` for filename without extension

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
