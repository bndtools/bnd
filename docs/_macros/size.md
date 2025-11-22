---
layout: default
class: Macro
title: size ( ';' LIST )*
summary: Count the total number of elements in one or more lists
---

## Summary

The `size` macro counts and returns the total number of elements across one or more comma-separated lists.

## Syntax

```
${size;<list>[;<list>...]}
```

## Parameters

- `list` - One or more comma-separated lists to count

## Behavior

- Splits each list on commas
- Counts all elements across all lists
- Returns total count as integer
- Empty elements are counted

## Examples

Count elements in a list:
```
${size;a,b,c,d}
# Returns: 4
```

Count multiple lists:
```
${size;red,green;blue,yellow}
# Returns: 4
```

Count packages:
```
package.count=${size;${packages}}
```

Conditional on list size:
```
${if;${size;${exports}};has-exports;no-exports}
```

## Use Cases

- Counting list elements
- Validating list sizes
- Conditional logic based on count
- Build statistics
- Dependency counting

## Notes

- Returns integer count
- Combines all provided lists
- Empty strings are counted as elements
- Maximum 16 lists (implementation limit)
- See also: `${length}` for string length

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
