---
layout: default
class: Project
title:	findname ';' REGEX ( ';' REPLACEMENT )?
summary: Find bundle resources by filename with optional regex replacement
---

## Summary

The `findname` macro finds resources in the current bundle by matching filenames (not full paths) against a regular expression, with optional replacement to transform the results.

## Syntax

```
${findname[;<regex>[;<replacement>]]}
```

## Parameters

- `regex` (optional) - Regular expression to match filenames (default: ".*" matches all)
- `replacement` (optional) - Replacement pattern using regex groups (e.g., "$1")

## Behavior

- Searches bundle resources by filename only (not full path)
- Matches filename against regex pattern
- Optionally applies replacement to matched names
- Returns comma-separated list of results

## Examples

Find all resources:
```
${findname}
```

Find by pattern:
```
${findname;.*\.properties}
# Returns filenames ending in .properties
```

Find and transform:
```
${findname;(.*)\.class;$1}
# Returns class names without .class extension
```

Extract parts of filenames:
```
${findname;config-(.*)\.xml;$1}
# Returns: "dev,prod,test" from "config-dev.xml", "config-prod.xml", etc.
```

## Use Cases

- Resource discovery by name
- Filename pattern matching
- Name transformation
- Bundle content analysis
- Dynamic resource lists

## Notes

- Matches filename only, not directory path
- Uses Java regex syntax
- See also: `${findpath}` for full path matching
- See also: `${lsr}` for file system searches



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
