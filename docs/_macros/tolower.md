---
layout: default
class: Macro
title: tolower STRING
summary: Convert a string to lowercase
---

## Summary

The `tolower` macro converts a string to lowercase using the default locale.

## Syntax

```
${tolower;<string>}
```

## Parameters

- `string` - The string to convert to lowercase

## Behavior

- Converts all uppercase letters to lowercase
- Uses default locale for conversion
- Non-letter characters remain unchanged
- Returns the lowercase string

## Examples

Convert simple string:
```
${tolower;HELLO WORLD}
# Returns: "hello world"
```

Convert mixed case:
```
${tolower;MyClassName}
# Returns: "myclassname"
```

Normalize for comparison:
```
${if;${equals;${tolower;${input}};${tolower;${expected}}};match;no-match}
```

Convert package names:
```
normalized=${tolower;Com.Example.API}
# Returns: "com.example.api"
```

## Use Cases

- Normalizing strings for comparison
- Converting user input to standard format
- Creating lowercase identifiers
- Case-insensitive matching
- Formatting output consistently

## Notes

- Uses default locale conversion rules
- See also: `${toupper}` for uppercase conversion
- Locale-sensitive for certain characters



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
