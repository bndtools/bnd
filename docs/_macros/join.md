---
layout: default
class: Macro
title: join ( ';' LIST )+
summary: Combine multiple lists into a single comma-separated list
---

## Summary

The `join` macro combines one or more lists into a single comma-separated list. It flattens all input lists and returns them joined with commas.

## Syntax

```
${join;<list>[;<list>...]}
```

## Parameters

- `list` - One or more lists to combine (items within lists can be comma or semicolon-separated)

## Behavior

- Splits each input list into individual elements
- Combines all elements from all lists
- Returns a single comma-separated list
- Preserves the order of elements
- Removes empty elements

## Examples

Join two lists:
```
${join;apple,banana;cherry,date}
# Returns: "apple,banana,cherry,date"
```

Join multiple lists:
```
${join;red,green;blue;yellow,orange}
# Returns: "red,green,blue,yellow,orange"
```

Flatten nested lists:
```
list1=a,b,c
list2=d,e,f
${join;${list1};${list2}}
# Returns: "a,b,c,d,e,f"
```

Combine with other macros:
```
${join;${exports};${imports}}
# Returns all exported and imported packages
```

## Use Cases

- Combining multiple configuration lists
- Merging classpath or dependency lists
- Concatenating package lists
- Building composite values from multiple sources
- Flattening nested list structures
- Creating unified collections

## Notes

- Input lists can use comma or semicolon separators
- Output is always comma-separated
- Empty elements are filtered out
- Element order is preserved
- No deduplication - duplicate elements are kept
- To join with a custom separator (not comma), use `${sjoin;<separator>;<list>...}` instead
- See also: `${uniq}` for removing duplicates
- See also: `${sjoin}` for custom separator joining


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
