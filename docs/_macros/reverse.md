---
layout: default
class: Macro
title: reverse (';' LIST )*
summary: Reverse the order of elements in one or more lists
---

## Summary

The `reverse` macro reverses the order of elements from one or more combined lists. The last element becomes first, and the first becomes last.

## Syntax

```
${reverse;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists (items within lists are comma-separated)

## Behavior

- Combines all provided lists into a single list
- Reverses the order of all elements
- Returns the reversed list as a comma-separated string

## Examples

Reverse a simple list:
```
${reverse;1,2,3,4,5}
# Returns: "5,4,3,2,1"
```

Reverse text items:
```
${reverse;apple,banana,cherry}
# Returns: "cherry,banana,apple"
```

Reverse multiple lists:
```
${reverse;red,green;blue,yellow}
# Returns: "yellow,blue,green,red"
```

Reverse file list:
```
${reverse;${lsr;src;*.java}}
# Returns files in reverse order
```

## Use Cases

- Reversing lists for processing in opposite order
- Creating descending sort orders
- Stack-like operations (LIFO)
- Reversing dependency orders
- Changing processing sequence
- List manipulation

## Notes

- The entire combined list is reversed
- Element values are not modified, only their order
- Empty lists return empty results
- Useful for reordering sorted lists
- Can be combined with `${sort}` for descending order


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
