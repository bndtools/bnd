---
layout: default
class: Macro
title: substring ';' STRING ';' START ( ';' END )?
summary: Extract a substring from a string with support for negative indices
---

## Summary

The `substring` macro extracts a portion of a string between start and end positions. It supports negative indices to count from the end of the string.

## Syntax

```
${substring;<string>;<start>[;<end>]}
```

## Parameters

- `string` - The source string
- `start` - Starting index (0-based, negative counts from end)
- `end` (optional) - Ending index (exclusive, negative counts from end, default: end of string)

## Behavior

- Extracts substring from start (inclusive) to end (exclusive)
- Negative indices count from the end (-1 = last character)
- If start > end, they are automatically swapped
- Default end is the string length
- Returns the extracted substring

## Examples

Get first 5 characters:
```
${substring;hello world;0;5}
# Returns: "hello"
```

Get last 5 characters:
```
${substring;hello world;-5}
# Returns: "world"
```

Remove first 2 characters:
```
${substring;hello;2}
# Returns: "llo"
```

Extract middle portion:
```
${substring;hello world;6;11}
# Returns: "world"
```

Use negative indices:
```
${substring;hello world;-5;-1}
# Returns: "worl"
```

## Use Cases

- Extracting prefixes or suffixes
- Removing fixed-length headers
- Parsing formatted strings
- Getting file extensions
- Truncating strings
- String manipulation in templates

## Notes

- Indices are 0-based (first character is 0)
- Negative indices count backwards from end
- End index is exclusive (not included in result)
- Start and end are swapped if start > end
- Throws exception if indices are out of bounds


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
