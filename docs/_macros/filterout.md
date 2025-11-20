---
layout: default
class: Macro
title: filterout ';' LIST ';' REGEX
summary: Filter a list to exclude entries matching a regular expression
---

## Summary

The `filterout` macro filters a list to exclude entries that match a specified regular expression pattern. This is the inverse of `${filter}` - entries that match the pattern are removed from the result.

## Syntax

```
${filterout;<list>;<regex>}
```

## Parameters

- `list` - A comma or semicolon-separated list of values
- `regex` - A Java regular expression pattern to match against

## Behavior

- Splits the input list into individual entries
- Compiles the regex pattern
- Tests each entry against the pattern using full match semantics
- Removes entries that match the pattern (keeps non-matching ones)
- Returns the filtered list as a comma-separated string
- Empty result if all entries match (all excluded)

## Examples

Exclude test files:
```
${filterout;Main.java,Test.java,TestUtil.java;.*Test.*}
# Returns: "Main.java"
```

Exclude snapshot versions:
```
${filterout;1.0.0,2.0.0-SNAPSHOT,3.0.0;.*SNAPSHOT.*}
# Returns: "1.0.0,3.0.0"
```

Exclude specific packages:
```
${filterout;com.example.api,com.example.impl,com.example.test;.*\.test}
# Returns: "com.example.api,com.example.impl"
```

Exclude by prefix:
```
${filterout;prod-bundle,test-bundle,dev-bundle;test-.*|dev-.*}
# Returns: "prod-bundle"
```

## Use Cases

- Removing test files from production builds
- Excluding snapshot versions from release lists
- Filtering out unwanted packages or dependencies
- Cleaning up file lists
- Removing debug or development artifacts
- Excluding internal implementation packages

## Notes

- Uses Java regular expression syntax
- The pattern must match the *entire* entry (full match, not substring)
- Case-sensitive by default (use `(?i)` for case-insensitive matching)
- Inverse operation of `${filter}` - keeps entries that DON'T match
- See also: `${reject}` which is an alias for `${filterout}`
- See also: `${filter}` (or `${select}`) for inclusive filtering

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
