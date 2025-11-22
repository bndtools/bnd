---
layout: default
class: Macro
title: nsort (';' LIST )+
summary: Sort lists numerically by treating values as numbers
---

## Summary

The `nsort` macro combines one or more lists and sorts their contents numerically. Unlike alphabetic sorting, it treats values as numbers, so "2" comes before "10".

## Syntax

```
${nsort;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists to sort (items within lists are comma-separated)

## Behavior

- Combines all provided lists into a single list
- Strips leading zeros from values for comparison
- Sorts primarily by numeric length (shorter numbers first)
- For values of equal length, sorts lexicographically
- Returns a comma-separated sorted list

## Examples

Sort numbers correctly:
```
${nsort;10,2,1,20,3}
# Returns: "1,2,3,10,20" (not "1,10,2,20,3")
```

Sort with leading zeros:
```
${nsort;001,010,002,100}
# Returns: "1,2,10,100" (leading zeros removed)
```

Combine and sort multiple lists:
```
${nsort;5,15,25;10,20,30}
# Returns: "5,10,15,20,25,30"
```

Sort version-like numbers:
```
${nsort;1,11,2,21,3}
# Returns: "1,2,3,11,21"
```

## Use Cases

- Sorting version numbers or identifiers
- Ordering numeric strings correctly
- Sorting file numbers or indices
- Version lists in proper numeric order
- Port numbers or numeric identifiers
- Any list where numeric order matters

## Notes

- Sorts by length first, then lexicographically
- Leading zeros are stripped before comparison
- All values are treated as strings during comparison
- Shorter numbers always come before longer numbers
- For equal-length values, uses string comparison
- See also: `${sort}` for alphabetic sorting
- See also: `${vcompare}` for semantic version comparison


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
