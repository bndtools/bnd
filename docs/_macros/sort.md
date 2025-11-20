---
layout: default
class: Macro
title: sort (';' LIST )+
summary: Sort lists alphabetically
---

## Summary

The `sort` macro combines one or more lists and sorts their contents alphabetically (lexicographically). The sorting is case-sensitive with uppercase letters before lowercase.

## Syntax

```
${sort;<list>[;<list>...]}
```

## Parameters

- `list` - One or more semicolon-separated lists to sort

## Behavior

- Combines all provided lists
- Sorts elements alphabetically
- Case-sensitive sorting (A-Z before a-z)
- Returns comma-separated sorted list

## Examples

Sort a simple list:
```
${sort;cherry,apple,banana}
# Returns: "apple,banana,cherry"
```

Sort with mixed case:
```
${sort;Zebra,apple,Ant,banana}
# Returns: "Ant,Zebra,apple,banana"
```

Sort multiple lists:
```
${sort;red,green;blue,yellow}
# Returns: "blue,green,red,yellow"
```

Sort and filter:
```
${sort;${filter;${packages};com\.example\..*}}
```

## Use Cases

- Organizing lists alphabetically
- Creating ordered reports
- Sorting package or class names
- Normalizing list order for comparison
- Generating sorted documentation

## Notes

- Case-sensitive lexicographic sorting
- Uppercase letters sort before lowercase
- For numeric sorting, use `${nsort}`
- See also: `${nsort}` for numeric sorting
- See also: `${reverse}` to reverse order



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
