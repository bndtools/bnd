---
layout: default
class: Macro
title: sum (';' LIST )*
summary: Calculate the sum of numeric values in one or more lists
---

## Summary

The `sum` macro calculates the sum of all numeric values provided in one or more lists. Each element is parsed as a double-precision floating-point number.

## Syntax

```
${sum;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists of numeric values

## Behavior

- Parses all elements as double-precision numbers
- Calculates the sum of all values
- Returns the total as a string
- Throws exception if any value is not numeric

## Examples

Sum a simple list:
```
${sum;1,2,3,4,5}
# Returns: "15"
```

Sum multiple lists:
```
${sum;10,20;30,40}
# Returns: "100"
```

Sum decimal values:
```
${sum;1.5,2.5,3.5}
# Returns: "7.5"
```

Calculate total from properties:
```
total=${sum;${value1};${value2};${value3}}
```

## Use Cases

- Calculating totals and aggregates
- Summing metric values
- Computing combined sizes or counts
- Mathematical calculations in builds
- Aggregating numeric configuration

## Notes

- All values must be numeric
- Returns double-precision result
- See also: `${average}` for mean calculation
- See also: `${max}` and `${min}` for extremes



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
