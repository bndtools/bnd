---
layout: default
class: Macro
title: max (';' LIST )*
summary: Find the maximum string in one or more lists
---

## Summary

The `max` macro compares strings lexicographically using Java's `String.compareTo()` and returns the maximum (highest) value.

## Syntax

```
${max;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists of strings

## Behavior

- Compares strings lexicographically
- Case-sensitive comparison
- Returns the highest string value
- Uses standard Java string ordering

## Examples

Find maximum string:
```
${max;apple,banana,cherry}
# Returns: "cherry"
```

Multiple lists:
```
${max;abc,xyz;def,mno}
# Returns: "xyz"
```

Numeric strings (lexicographic):
```
${max;1,10,2,20}
# Returns: "20" (lexicographic, not numeric)
```

## Use Cases

- Finding highest string value
- String comparison
- Alphabetic sorting (last)
- String selection
- Data analysis

## Notes

- Lexicographic (dictionary) ordering
- Case-sensitive ("Z" < "a")
- Not numeric comparison
- For numeric max, use proper number comparison
- See also: `${min}` for minimum
- See also: `${vmax}` for version comparison
- See also: `${nmax}` for numeric comparison


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
