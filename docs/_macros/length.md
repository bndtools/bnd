---
layout: default
class: Macro
title: length STRING
summary: Get the length of a string in characters
---

## Summary

The `length` macro returns the number of characters in a string. Returns 0 if no argument is provided.

## Syntax

```
${length[;<string>]}
```

## Parameters

- `string` (optional) - The string to measure. If omitted, returns 0.

## Behavior

- Returns the character count
- Returns 0 if no string provided
- Counts all characters including spaces
- Unicode characters count as single characters

## Examples

Get string length:
```
${length;hello world}
# Returns: 11
```

Check if empty:
```
${if;${length;${value}};has-value;empty}
```

Validate input length:
```
${if;${length;${password}};password-ok;password-required}
```

No argument:
```
${length}
# Returns: 0
```

## Use Cases

- String length validation
- Checking for empty strings
- Input validation
- String size limits
- Conditional logic based on length

## Notes

- Returns integer count
- Includes all characters (spaces, newlines, etc.)
- Unicode safe
- See also: `${size}` for list element count


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
