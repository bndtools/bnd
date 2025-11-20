---
layout: default
class: Project
title: findpath ';' REGEX ( ';' REPLACE )?
summary: Find bundle resources by full path with optional regex replacement
---

## Summary

The `findpath` macro finds resources in the current bundle by matching full resource paths against a regular expression, with optional replacement to transform the results.

## Syntax

```
${findpath[;<regex>[;<replacement>]]}
```

## Parameters

- `regex` (optional) - Regular expression to match full paths (default: ".*" matches all)
- `replacement` (optional) - Replacement pattern using regex groups (e.g., "$1")

## Behavior

- Searches bundle resources by full path
- Matches entire path against regex pattern
- Optionally applies replacement to matched paths
- Returns comma-separated list of results

## Examples

Find all resources:
```
${findpath}
# Returns full paths of all resources
```

Find by path pattern:
```
${findpath;META-INF/.*\.xml}
# Returns XML files (full path) in META-INF/
```

Extract path components:
```
${findpath;com/example/(.*)/.*\.class;$1}
# Returns middle package component
```

Find and transform:
```
${findpath;src/(.*)\.java;$1}
# Returns paths without src/ prefix and .java extension
```

## Use Cases

- Resource discovery by path
- Full path pattern matching
- Path transformation
- Bundle content analysis
- Package structure extraction

## Notes

- Matches full resource path, not just filename
- Uses Java regex syntax
- See also: `${findname}` for filename-only matching
- See also: `${lsr}` for file system searches



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
