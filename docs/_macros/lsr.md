---
layout: default
class: Macro
title: lsr ';' DIR (';' SELECTORS )
summary: List files with relative paths, optionally filtered
---

## Summary

The `lsr` macro returns a comma-separated list of relative file paths from a directory, with optional filtering using selectors.

## Syntax

```
${lsr;<directory>[;<selector>...]}
```

## Parameters

- `directory` - Directory path to list (relative to project base)
- `selector` (optional) - One or more file selectors/patterns to filter results

## Behavior

- Lists files in the specified directory
- Returns relative paths (from the directory)
- Optional filtering with selectors
- Comma-separated output
- Recursive by default

## Examples

List all files:
```
${lsr;src/main/java}
# Returns relative paths
```

Filter by pattern:
```
${lsr;lib;*.jar}
# Lists all JAR files with relative paths
```

Find source files:
```
${lsr;src;*.java}
# All Java source files
```

Multiple patterns:
```
${lsr;resources;*.xml;*.properties}
```

## Use Cases

- File discovery with relative paths
- Source file collection
- Resource listing
- Build inputs
- Pattern-based file selection

## Notes

- Returns relative paths
- Recursive listing
- Selectors use file patterns
- See also: `${lsa}` for absolute paths
- See also: `${glob}` for pattern matching
- See also: `${findfile}` for filtered recursive search

<hr />
TODO Needs review - AI Generated content

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
