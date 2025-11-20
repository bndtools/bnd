---
layout: default
class: Processor
title: random
summary: Generate a random string that is a valid Java identifier
---

## Summary

The `random` macro generates a random string that is guaranteed to be a valid Java identifier. The first character is always a letter, and subsequent characters are letters or numbers.

## Syntax

```
${random[;<length>]}
```

## Parameters

- `length` (optional) - Number of characters to generate (default: 8)

## Behavior

- Generates a random string of specified length
- First character is always an ASCII letter (a-z, A-Z)
- Subsequent characters are ASCII letters or numbers (a-z, A-Z, 0-9)
- Result is always a valid Java identifier
- Default length is 8 characters

## Examples

Generate default 8-character identifier:
```
${random}
# Returns: "aB3xY9kL" (example)
```

Generate specific length:
```
${random;12}
# Returns: "K5mPqR7sT2uV" (example, 12 chars)
```

Create unique package name:
```
test.package=${random;16}
```

Generate unique property:
```
temp.id.${random;6}=value
```

Create unique class names:
```
TestClass${random;4}
```

## Use Cases

- Generating unique identifiers
- Creating temporary package or class names
- Generating random test data
- Creating unique property keys
- Avoiding naming conflicts
- Temporary file or directory naming

## Notes

- Output is always a valid Java identifier
- First character is never a number
- Uses only ASCII letters and numbers (no special characters)
- Not cryptographically secure - don't use for security purposes
- Each invocation generates a new random value
- See also: `${rand}` for random numbers


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
