---
layout: default
class: Macro
title: first (';' LIST )*
summary: Get the first element from one or more lists
---

## Summary

The `first` macro returns the first element from one or more comma-separated lists. If multiple lists are provided, it returns the first element from the combined lists.

## Syntax

```
${first;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists of values (items within each list are comma-separated)

## Behavior

- Combines all provided lists into a single list
- Returns the first element from the combined list
- Returns an empty string if all lists are empty
- Preserves the original value (no trimming or transformation)

## Examples

Get first element from a list:
```
${first;apple,banana,cherry}
# Returns: "apple"
```

Get first from multiple lists:
```
${first;red,green;blue,yellow}
# Returns: "red"
```

Get first non-empty value:
```
version=${first;${custom.version};${default.version};1.0.0}
# Returns first defined version
```

Use with property lists:
```
primary.jar=${first;${buildpath}}
```

Select first matching file:
```
config.file=${first;${lsr;config;*.properties}}
```

## Use Cases

- Selecting the primary item from a list
- Getting default values (first defined wins)
- Extracting lead elements for processing
- Choosing the first available option
- Implementing fallback chains
- Getting the head of a sequence

## Notes

- Returns empty string (not null) for empty lists
- Does not modify or trim the value
- Can handle multiple list arguments separated by semicolons
- See also: `${last}` for getting the last element
- See also: `${sublist}` for extracting multiple elements


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
