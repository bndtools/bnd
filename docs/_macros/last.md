---
layout: default
class: Macro
title: last (';' LIST )*
summary: Get the last element from one or more lists
---

## Summary

The `last` macro returns the last element from one or more comma-separated lists. If multiple lists are provided, it returns the last element from the combined lists.

## Syntax

```
${last;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists of values (items within each list are comma-separated)

## Behavior

- Combines all provided lists into a single list
- Returns the last element from the combined list
- Returns an empty string if all lists are empty
- Preserves the original value (no trimming or transformation)

## Examples

Get last element from a list:
```
${last;apple,banana,cherry}
# Returns: "cherry"
```

Get last from multiple lists:
```
${last;red,green;blue,yellow}
# Returns: "yellow"
```

Get last defined value:
```
version=${last;1.0.0;${custom.version};${snapshot.version}}
# Returns last defined version
```

Use with property lists:
```
latest.jar=${last;${buildpath}}
```

Select last matching file:
```
newest=${last;${sort;${lsr;logs;*.log}}}
```

## Use Cases

- Selecting the final item from a list
- Getting the most recent value (when lists are ordered)
- Extracting tail elements for processing
- Choosing the last available option
- Implementing last-wins strategies
- Getting the tail of a sequence

## Notes

- Returns empty string (not null) for empty lists
- Does not modify or trim the value
- Can handle multiple list arguments separated by semicolons
- See also: `${first}` for getting the first element
- See also: `${get}` with negative index (-1) for the same result
- See also: `${sublist}` for extracting multiple elements



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
