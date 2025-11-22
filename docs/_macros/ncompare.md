---
layout: default
class: Macro
title: ncompare NUMBER NUMBER
summary: Compare two numbers by using the Double.compare method.
---

## Summary

Compare two numbers numerically using Java's `Double.compare()` method, returning -1, 0, or 1.

## Syntax

    ${ncompare;<number1>;<number2>}

## Parameters

- **number1**: First number to compare (can be integer or floating-point)
- **number2**: Second number to compare (can be integer or floating-point)

## Behavior

Compares the two numbers numerically and returns:
- `0` - The numbers are equal
- `1` - The first number is greater than the second
- `-1` - The first number is less than the second

Numbers are parsed as doubles, supporting both integers and floating-point values.

## Examples

```
# Equal numbers
${ncompare;42;42}
# Returns: 0

# First number greater
${ncompare;100;50}
# Returns: 1

# First number less
${ncompare;10;20}
# Returns: -1

# Floating-point comparison
${ncompare;3.14;2.71}
# Returns: 1

# Negative numbers
${ncompare;-5;-10}
# Returns: 1 (-5 is greater than -10)

# Use in conditional logic
-include ${if;${ncompare;${Bundle-Version};2.0};modern.bnd;legacy.bnd}

# Compare with zero
${ncompare;${errorcount};0}
# Returns: 1 if errors exist, 0 if no errors

# Numeric comparison (not lexicographic)
${ncompare;10;2}
# Returns: 1 (10 is greater than 2 numerically)

# Compare with macro values
${ncompare;${size;${errors}};5}
# Check if more than 5 errors
```

## Use Cases

1. **Threshold Checks**: Verify if a value exceeds a threshold
2. **Conditional Builds**: Make decisions based on numeric values
3. **Validation**: Check if counts, sizes, or versions meet requirements
4. **Quality Gates**: Ensure numeric metrics are within acceptable ranges
5. **Build Optimization**: Skip steps based on numeric conditions

## Notes

- Comparison is **numeric**, not lexicographic
- For string comparison, use [compare](compare.html)
- For OSGi version comparison, use [vcompare](vcompare.html)
- Both arguments are parsed as double-precision floating-point numbers
- Returns only -1, 0, or 1
- Handles integers, decimals, and negative numbers
- Useful with [if](if.html) macro for conditional logic

## Related Macros

- [compare](compare.html) - Compare strings lexicographically
- [vcompare](vcompare.html) - Compare OSGi version strings
- [if](if.html) - Conditional macro that can use comparison results
- [size](size.html) - Get list size for numeric comparisons



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
