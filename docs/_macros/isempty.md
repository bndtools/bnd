---
layout: default
class: Macro
title: isempty ( ';' STRING )* 
summary: Check if all given strings are empty
---

## Summary

The `isempty` macro checks if all provided arguments are empty strings. Returns true only if all values are empty, false otherwise.

## Syntax

```
${isempty[;<string>...]}
```

## Parameters

- `string` (optional) - Zero or more strings to check

## Behavior

- Returns true if all arguments are empty strings
- Returns true if no arguments provided
- Returns false if any argument has content
- Empty string means zero length

## Examples

Check if empty:
```
${if;${isempty;${value}};value-empty;value-has-content}
```

Check multiple values:
```
${isempty;;;;}
# Returns: true (all empty)
```

Validate required fields:
```
${if;${isempty;${name};${email}};missing-fields;fields-ok}
```

No arguments:
```
${isempty}
# Returns: true
```

## Use Cases

- Input validation
- Checking for missing values
- Required field validation
- Conditional logic on emptiness
- Form validation

## Notes

- All arguments must be empty for true
- Empty string is ""
- Whitespace is not empty
- Returns boolean
- See also: `${length}` to check string size
- See also: `${def}` for default values

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
