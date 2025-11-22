---
layout: default
class: Macro
title: compare STRING STRING
summary: Compare two strings by using the compareTo method of the String class.
---

## Summary

Compare two strings lexicographically using Java's `String.compareTo()` method, returning -1, 0, or 1.

## Syntax

    ${compare;<string1>;<string2>}

## Parameters

- **string1**: First string to compare
- **string2**: Second string to compare

## Behavior

Compares the two strings lexicographically (alphabetically) and returns:
- `0` - The strings are equal
- `1` - The first string is greater than the second (comes later alphabetically)
- `-1` - The first string is less than the second (comes earlier alphabetically)

The comparison is case-sensitive and uses Unicode character values.

## Examples

```
# Equal strings
${compare;hello;hello}
# Returns: 0

# First string greater
${compare;world;hello}
# Returns: 1

# First string less
${compare;apple;banana}
# Returns: -1

# Case-sensitive comparison
${compare;Apple;apple}
# Returns: -1 (uppercase comes before lowercase)

# Use in conditional logic
-include ${if;${compare;${env;MODE};production};prod.bnd;dev.bnd}

# Numeric strings (lexicographic, not numeric)
${compare;10;2}
# Returns: -1 (because "1" < "2" alphabetically)

# Empty string comparison
${compare;;text}
# Returns: -1 (empty string is less than any non-empty string)
```

## Use Cases

1. **Conditional Logic**: Make decisions based on string comparison
2. **Sorting Logic**: Implement custom sort orders in build configurations
3. **Version Comparison**: Compare simple version strings (use [vcompare](vcompare.html) for OSGi versions)
4. **Configuration Selection**: Choose configuration based on string values
5. **Validation**: Check if strings match expected values

## Notes

- Comparison is **lexicographic** (alphabetical), not numeric
- For numeric comparison, use [ncompare](ncompare.html)
- For OSGi version comparison, use [vcompare](vcompare.html)
- Comparison is case-sensitive
- Returns only -1, 0, or 1 (not the actual difference between strings)
- Useful with [if](if.html) macro for conditional builds

## Related Macros

- [ncompare](ncompare.html) - Compare numbers numerically
- [vcompare](vcompare.html) - Compare OSGi version strings
- [if](if.html) - Conditional macro that can use comparison results



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
