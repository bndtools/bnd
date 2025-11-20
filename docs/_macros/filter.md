---
layout: default
class: Macro
title: filter ';' LIST ';' REGEX
summary: Filter a list to include only entries matching a regular expression
---

## Summary

The `filter` macro filters a list to include only those entries that match a specified regular expression pattern. Entries that don't match are removed from the result.

## Syntax

```
${filter;<list>;<regex>}
```

## Parameters

- `list` - A comma or semicolon-separated list of values
- `regex` - A Java regular expression pattern to match against

## Behavior

- Splits the input list into individual entries
- Compiles the regex pattern
- Tests each entry against the pattern using full match semantics
- Keeps only entries that match the pattern
- Returns the filtered list as a comma-separated string
- Empty result if no entries match

## Examples

Filter for JAR files:
```
${filter;foo.jar,bar.txt,baz.jar;.*\.jar}
# Returns: "foo.jar,baz.jar"
```

Filter package names:
```
${filter;com.example.api,com.example.impl,org.other;com\.example\..*}
# Returns: "com.example.api,com.example.impl"
```

Filter versions:
```
${filter;1.0.0,2.0.0-SNAPSHOT,2.1.0;.*SNAPSHOT.*}
# Returns: "2.0.0-SNAPSHOT"
```

Filter by prefix:
```
${filter;test-bundle,main-bundle,test-util;test-.*}
# Returns: "test-bundle,test-util"
```

## Use Cases

- Selecting specific files from a list
- Filtering packages or classes by naming patterns
- Extracting specific versions from a list
- Building selective classpaths
- Conditional inclusion based on naming conventions
- Cleaning up lists to match specific criteria

## Notes

- Uses Java regular expression syntax
- The pattern must match the *entire* entry (full match, not substring)
- Case-sensitive by default (use `(?i)` for case-insensitive matching)
- See also: `${filterout}` (or `${reject}`) for inverse filtering
- See also: `${select}` which is an alias for `${filter}`


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
