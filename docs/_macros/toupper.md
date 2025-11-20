---
layout: default
class: Macro
title: toupper STRING
summary: Convert a string to uppercase
---

## Summary

The `toupper` macro converts a string to uppercase using the default locale.

## Syntax

```
${toupper;<string>}
```

## Parameters

- `string` - The string to convert to uppercase

## Behavior

- Converts all lowercase letters to uppercase
- Uses default locale for conversion
- Non-letter characters remain unchanged
- Returns the uppercase string

## Examples

Convert simple string:
```
${toupper;hello world}
# Returns: "HELLO WORLD"
```

Convert mixed case:
```
${toupper;MyClassName}
# Returns: "MYCLASSNAME"
```

Create constants:
```
constant.name=${toupper;${property.name}}
```

Format identifiers:
```
Bundle-Id: ${toupper;${bsn}}
```

## Use Cases

- Creating uppercase constants
- Normalizing strings for comparison
- Formatting identifiers
- Case-insensitive matching
- Standard formatting requirements

## Notes

- Uses default locale conversion rules
- See also: `${tolower}` for lowercase conversion
- Locale-sensitive for certain characters



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
