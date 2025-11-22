---
layout: default
class: Macro
title: rand (';' MIN ' (;' MAX )?)?
summary: Generate a random number within a specified range
---

## Summary

The `rand` macro generates a random integer. By default it returns a number between 0 and 100, or within a specified min/max range (inclusive).

## Syntax

```
${rand[;<max>[;<min>]]}
```

## Parameters

- `max` (optional) - Maximum value (default: 100)
- `min` (optional) - Minimum value (default: 0)

## Behavior

- No arguments: Random number 0-100
- One argument: Random number 0 to max
- Two arguments: Random number min to max
- Range is inclusive (includes both min and max)
- Returns rounded integer

## Examples

Default range (0-100):
```
${rand}
# Returns: 42 (example)
```

Custom max:
```
${rand;10}
# Returns: 7 (between 0-10)
```

Custom range:
```
${rand;100;50}
# Returns: 73 (between 50-100)
```

Generate unique build number:
```
build.number=${rand;9999;1000}
```

## Use Cases

- Generating random test data
- Creating unique identifiers
- Random selection
- Build numbers
- Temporary values

## Notes

- Range is inclusive on both ends
- Returns integer (rounded)
- Not cryptographically secure
- Each invocation generates new value
- See also: `${random}` for random strings


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
