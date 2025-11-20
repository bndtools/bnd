---
layout: default
class: Macro
title: min (';' LIST )*
summary: Find the minimum string in one or more lists
---

## Summary

The `min` macro compares strings lexicographically using Java's `String.compareTo()` and returns the minimum (lowest) value.

## Syntax

```
${min;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists of strings

## Behavior

- Compares strings lexicographically
- Case-sensitive comparison
- Returns the lowest string value
- Uses standard Java string ordering

## Examples

Find minimum string:
```
${min;apple,banana,cherry}
# Returns: "apple"
```

Multiple lists:
```
${min;xyz,abc;mno,def}
# Returns: "abc"
```

Numeric strings (lexicographic):
```
${min;1,10,2,20}
# Returns: "1" (lexicographic, not numeric)
```

## Use Cases

- Finding lowest string value
- String comparison
- Alphabetic sorting (first)
- String selection
- Data analysis

## Notes

- Lexicographic (dictionary) ordering
- Case-sensitive ("A" < "a")
- Not numeric comparison
- For numeric min, use proper number comparison
- See also: `${max}` for maximum
- See also: `${vmin}` for version comparison
- See also: `${nmin}` for numeric comparison


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
