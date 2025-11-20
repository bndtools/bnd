---
layout: default
class: Macro
title: sublist ';' START ';' END (';' LIST )*
summary: Extract a portion of a list with support for negative indices
---

## Summary

The `sublist` macro extracts elements from a list between start and end positions. It supports negative indices to count from the end of the list.

## Syntax

```
${sublist;<start>;<end>;<list>[;<list>...]}
```

## Parameters

- `start` - Starting index (0-based, negative counts from end)
- `end` - Ending index (exclusive, negative counts from end)
- `list` - One or more lists to combine and extract from

## Behavior

- Combines all provided lists
- Extracts elements from start (inclusive) to end (exclusive)
- Negative indices count from the end
- If start > end, they are automatically swapped
- Returns comma-separated sublist

## Examples

Get first 3 elements:
```
${sublist;0;3;apple,banana,cherry,date,elderberry}
# Returns: "apple,banana,cherry"
```

Get last 2 elements:
```
${sublist;-2;-0;apple,banana,cherry}
# Returns: "banana,cherry"
```

Skip first element:
```
${sublist;1;-0;red,green,blue}
# Returns: "green,blue"
```

Get middle elements:
```
${sublist;2;5;a,b,c,d,e,f,g}
# Returns: "c,d,e"
```

Extract from multiple lists:
```
${sublist;0;3;one,two;three,four,five}
# Returns: "one,two,three"
```

## Use Cases

- Extracting ranges from lists
- Pagination of results
- Removing headers or trailers
- List slicing operations
- Selecting specific segments
- Windowing over lists

## Notes

- Indices are 0-based (first element is 0)
- Negative indices count backwards (-1 adjusts to list end)
- End index is exclusive (not included in result)
- Start and end are swapped if start > end
- Combines multiple list arguments first
- See also: `${first}`, `${last}`, `${get}` for single elements



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
