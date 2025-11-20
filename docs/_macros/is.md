---
layout: default
class: Macro
title: is ( ';' ANY )* 
summary: Check if all given values are equal
---

## Summary

The `is` macro checks if all provided arguments are equal to each other. Returns true if all values match, false otherwise.

## Syntax

```
${is;<value>;<value>[;<value>...]}
```

## Parameters

- `value` - Two or more values to compare (minimum 2 required)

## Behavior

- Compares all arguments using string equality
- Returns true only if all values are exactly equal
- Requires at least 2 arguments
- Case-sensitive comparison

## Examples

Check equality of two values:
```
${if;${is;${version};1.0.0};version-match;version-mismatch}
```

Compare multiple values:
```
${is;abc;abc;abc}
# Returns: true
```

Check property equality:
```
${if;${is;${env};production};prod-config;dev-config}
```

Validate configuration:
```
${is;${driver};gradle;gradle}
# Returns: true if driver is gradle
```

## Use Cases

- Validating that values match
- Conditional logic based on equality
- Configuration validation
- Multi-value consistency checks
- String comparison in conditionals

## Notes

- All arguments must be exactly equal (string comparison)
- Case-sensitive
- Empty strings are valid values
- See also: `${equals}` for two-value comparison
- Useful with `${if}` for conditional logic



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
