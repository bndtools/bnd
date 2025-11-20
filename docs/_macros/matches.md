---
layout: default
class: Macro
title: matches STRING REGEX
summary: Check if a string matches a regular expression pattern
---

## Summary

The `matches` macro tests whether a string matches a given regular expression pattern. Returns true if the entire string matches, false otherwise.

## Syntax

```
${matches;<string>;<regex>}
```

## Parameters

- `string` - The string to test
- `regex` - The regular expression pattern (Java regex syntax)

## Behavior

- Tests if entire string matches the pattern
- Returns boolean (true/false)
- Uses Java regular expression syntax
- Must match the entire string (implicit anchoring)

## Examples

Check pattern match:
```
${if;${matches;v1.2.3;v[0-9]+\.[0-9]+\.[0-9]+};valid-version;invalid}
```

Validate format:
```
${matches;com.example.api;com\.example\..*}
# Returns: true
```

Check numeric:
```
${if;${matches;${value};[0-9]+};is-number;not-number}
```

Test package pattern:
```
${matches;${package};com\..*\.impl}
```

## Use Cases

- Pattern validation
- String format checking
- Conditional logic based on patterns
- Input validation
- Package name filtering

## Notes

- Uses Java regex syntax
- Entire string must match (no partial matches)
- Case-sensitive by default
- For case-insensitive, use `(?i)` in pattern
- See also: `${filter}` for list filtering
- See also: `${find}` for substring search



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
