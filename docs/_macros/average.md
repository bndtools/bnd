---
layout: default
class: Macro
title: average (';' LIST )*
summary: Calculate the arithmetic mean (average) of numeric values in one or more lists
---

## Summary

The `average` macro calculates the arithmetic mean of all numeric values provided in one or more semicolon-separated lists. Each list element is parsed as a double-precision floating-point number, and the result is the sum of all values divided by the count.

## Syntax

```
${average;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists of numeric values. Values within each list can be comma-separated.

## Behavior

- Parses all list elements as double-precision floating-point numbers
- Calculates the sum of all values across all lists
- Divides by the total number of values to get the average
- Throws an exception if no values are provided

## Examples

Calculate average of a simple list:
```
${average;1,2,3,4,5}
# Returns: 3
```

Calculate average across multiple lists:
```
${average;10,20,30;40,50}
# Returns: 30 (average of 10,20,30,40,50)
```

Calculate average of decimal values:
```
${average;1.5,2.5,3.5}
# Returns: 2.5
```

## Use Cases

- Computing average build times or metric values
- Calculating mean version numbers for analysis
- Aggregating numeric configuration values
- Statistical analysis of build data



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
