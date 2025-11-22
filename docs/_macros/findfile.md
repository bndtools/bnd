---
layout: default
class: Project
title:	findfile ';' PATH ( ';' FILTER )
summary: Get filtered list of file paths from a directory tree
---

## Summary

The `findfile` macro recursively searches a directory and returns a filtered list of relative file paths matching the specified filter pattern.

## Syntax

```
${findfile;<directory>[;<filter>]}
```

## Parameters

- `directory` - Base directory to search (relative to project)
- `filter` (optional) - Instruction pattern to match files (default: all files)

## Behavior

- Recursively traverses the directory tree
- Returns relative paths from the base directory
- Filters paths using instruction pattern matching
- Returns comma-separated list of matching paths

## Examples

Find all files:
```
${findfile;src}
```

Find Java source files:
```
${findfile;src;*.java}
```

Find with exclusion pattern:
```
${findfile;resources;!*.tmp}
```

## Use Cases

- File discovery
- Build input collection
- Resource gathering
- Pattern-based file selection

## Notes

- Paths are relative to the base directory
- Uses bnd instruction pattern matching
- See also: `${findname}`, `${findpath}`



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
