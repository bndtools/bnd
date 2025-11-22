---
layout: default
class: Macro
title: nmin (';' LIST )*
summary: Find the minimum number in one or more lists
---

## Summary

The `nmin` macro compares numeric values using floating-point comparison and returns the minimum (lowest) number.

## Syntax

```
${nmin;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists of numeric values

## Behavior

- Parses all values as doubles
- Compares numerically (not lexicographically)
- Returns the lowest number
- Handles integers and decimals

## Examples

Find minimum number:
```
${nmin;10,5,20,3}
# Returns: "3"
```

Multiple lists:
```
${nmin;100,200;50,150}
# Returns: "50"
```

Decimals:
```
${nmin;1.5,2.3,0.9}
# Returns: "0.9"
```

## Use Cases

- Finding minimum numeric value
- Numeric comparison
- Statistics and analysis
- Threshold calculations
- Data processing

## Notes

- Numeric comparison (not string)
- Handles negative numbers
- Floating-point precision
- See also: `${nmax}` for maximum
- See also: `${min}` for string comparison
- See also: `${vmin}` for version comparison


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
