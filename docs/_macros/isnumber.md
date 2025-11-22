---
layout: default
class: Macro
title: isnumber ( ';' STRING )* 
summary: Check if all given strings are valid numbers
---

## Summary

The `isnumber` macro checks if all provided arguments are valid numeric values. Returns true only if all arguments are numbers, false otherwise.

## Syntax

```
${isnumber;<string>[;<string>...]}
```

## Parameters

- `string` - One or more strings to check (minimum 1 required)

## Behavior

- Returns true if all arguments are valid numbers
- Returns false if any argument is not numeric
- Accepts integers and decimals
- Scientific notation supported
- Requires at least one argument

## Examples

Check if numeric:
```
${if;${isnumber;${value}};is-numeric;not-numeric}
```

Validate multiple values:
```
${isnumber;123;45.6;-78}
# Returns: true
```

Not a number:
```
${isnumber;abc}
# Returns: false
```

Mixed values:
```
${isnumber;123;abc}
# Returns: false (all must be numeric)
```

## Use Cases

- Input validation
- Type checking
- Numeric validation before operations
- Configuration validation
- Data type verification

## Notes

- All arguments must be numeric for true
- Supports integers, decimals, negative
- Scientific notation accepted
- Requires at least one argument
- Returns boolean
- See also: `${matches}` for pattern matching


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
