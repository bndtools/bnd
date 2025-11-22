---
layout: default
class: Macro
title: nmax (';' LIST )*
summary: Find the maximum number in one or more lists
---

## Summary

The `nmax` macro compares numeric values using floating-point comparison and returns the maximum (highest) number.

## Syntax

```
${nmax;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists of numeric values

## Behavior

- Parses all values as doubles
- Compares numerically (not lexicographically)
- Returns the highest number
- Handles integers and decimals

## Examples

Find maximum number:
```
${nmax;10,5,20,3}
# Returns: "20"
```

Multiple lists:
```
${nmax;100,200;50,150}
# Returns: "200"
```

Decimals:
```
${nmax;1.5,2.3,0.9}
# Returns: "2.3"
```

## Use Cases

- Finding maximum numeric value
- Numeric comparison
- Statistics and analysis
- Peak value detection
- Data processing

## Notes

- Numeric comparison (not string)
- Handles negative numbers
- Floating-point precision
- See also: `${nmin}` for minimum
- See also: `${max}` for string comparison
- See also: `${vmax}` for version comparison


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
