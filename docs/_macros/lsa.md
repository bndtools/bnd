---
layout: default
class: Macro
title: lsa ';' DIR (';' SELECTORS )
summary: List files with absolute paths, optionally filtered
---

## Summary

The `lsa` macro returns a comma-separated list of absolute file paths from a directory, with optional filtering using selectors.

## Syntax

```
${lsa;<directory>[;<selector>...]}
```

## Parameters

- `directory` - Directory path to list (relative to project base)
- `selector` (optional) - One or more file selectors/patterns to filter results

## Behavior

- Lists files in the specified directory
- Returns absolute paths
- Optional filtering with selectors
- Comma-separated output
- Non-recursive (single directory level)

## Examples

List all files:
```
${lsa;src/main/java}
# Returns absolute paths
```

Filter by pattern:
```
${lsa;lib;*.jar}
# Lists all JAR files with absolute paths
```

Multiple filters:
```
${lsa;resources;*.xml;*.properties}
```

Use in manifest:
```
Resources: ${lsa;config}
```

## Use Cases

- File discovery with absolute paths
- Build input collection
- Resource gathering
- File listing for processing
- Dependency collection

## Notes

- Returns absolute paths
- Non-recursive listing
- Selectors use file patterns
- See also: `${lsr}` for relative paths
- See also: `${glob}` for pattern matching
- See also: `${findfile}` for recursive search

<hr />
TODO Needs review - AI Generated content

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
