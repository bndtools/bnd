---
layout: default
class: Macro
title: trim ';' STRING 
summary: Remove leading and trailing whitespace from a string
---

## Summary

The `trim` macro removes leading and trailing whitespace from a string. It uses Java's `String.trim()` method, which removes spaces, tabs, newlines, and other whitespace characters.

## Syntax

```
${trim;<string>}
```

## Parameters

- `string` - The string to trim

## Behavior

- Removes leading whitespace characters
- Removes trailing whitespace characters
- Returns the trimmed string
- Preserves whitespace within the string

## Examples

Remove extra spaces:
```
${trim;  hello world  }
# Returns: "hello world"
```

Clean up property value:
```
cleaned.value=${trim;${some.property}}
```

Trim after concatenation:
```
${trim;${first} ${second} }
```

Process multiline value:
```
${trim;
    some value
}
# Returns: "some value"
```

## Use Cases

- Cleaning up user input
- Normalizing property values
- Removing accidental whitespace
- Processing template output
- Formatting string values
- Preparing strings for comparison

## Notes

- Only removes leading and trailing whitespace
- Internal whitespace is preserved
- Whitespace includes spaces, tabs, newlines, etc.
- Uses Java's definition of whitespace
- Empty string after trim remains empty



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
