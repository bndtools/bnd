---
layout: default
class: Macro
title: get ';' INDEX (';' LIST )*
summary: Get an element from a list at a specific index
---

## Summary

The `get` macro retrieves an element from one or more lists at a specified index position. It supports negative indices to count from the end of the list.

## Syntax

```
${get;<index>;<list>[;<list>...]}
```

## Parameters

- `index` - The zero-based position of the element to retrieve (negative values count from the end)
- `list` - One or more semicolon-separated lists (items within lists are comma-separated)

## Behavior

- Combines all provided lists into a single list
- Uses zero-based indexing (0 = first element, 1 = second element, etc.)
- Supports negative indices (-1 = last element, -2 = second-to-last, etc.)
- Throws an exception if the index is out of bounds

## Examples

Get first element (index 0):
```
${get;0;apple,banana,cherry}
# Returns: "apple"
```

Get second element:
```
${get;1;red,green,blue}
# Returns: "green"
```

Get last element (negative index):
```
${get;-1;one,two,three}
# Returns: "three"
```

Get second-to-last element:
```
${get;-2;alpha,beta,gamma,delta}
# Returns: "gamma"
```

Get from multiple lists:
```
${get;3;red,green;blue,yellow}
# Returns: "yellow" (4th element from combined list)
```

## Use Cases

- Extracting specific elements from lists
- Getting the first or last element with predictable position
- Accessing elements by position in configuration
- Selecting specific items from generated lists
- Array-like access to list elements
- Implementing ordered selections

## Notes

- Index is zero-based (0 is the first element)
- Negative indices are supported (-1 is the last element)
- Throws IndexOutOfBoundsException if index is invalid
- For first element, consider using `${first}` macro
- For last element, consider using `${last}` macro
- See also: `${sublist}` for extracting multiple elements


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
