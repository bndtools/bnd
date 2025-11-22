---
layout: default
class: Macro
title: uniq (';' LIST )*
summary: Remove duplicate elements from one or more lists
---

## Summary

The `uniq` macro combines one or more lists and removes all duplicate elements, preserving insertion order (first occurrence is kept).

## Syntax

```
${uniq;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists to process

## Behavior

- Combines all provided lists
- Removes duplicate elements
- Preserves insertion order (first occurrence kept)
- Returns comma-separated list of unique elements

## Examples

Remove duplicates from single list:
```
${uniq;1,2,3,1,2,4}
# Returns: "1,2,3,4"
```

Remove duplicates from multiple lists:
```
${uniq;red,green,blue;red,yellow}
# Returns: "red,green,blue,yellow"
```

Clean package list:
```
${uniq;${exports};${imports}}
# Returns unique packages
```

Deduplicate configuration:
```
unique.values=${uniq;${list1};${list2};${list3}}
```

## Use Cases

- Removing duplicate entries from lists
- Merging lists without duplicates
- Creating unique sets from multiple sources
- Cleaning up configuration values
- Deduplicating package or class lists
- Set operations on lists

## Notes

- Preserves insertion order (LinkedHashSet behavior)
- First occurrence of duplicate is kept
- Case-sensitive comparison
- Empty elements are preserved if present
- See also: `${sort}` to also sort the result
- See also: `${removeall}` and `${retainall}` for set operations


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
