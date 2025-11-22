---
layout: default
class: Macro
title: lastindexof ';' STRING (';' LIST )*
summary: Find the last index position of a value in one or more lists
---

## Summary

The `lastindexof` macro searches for a value in combined lists and returns the 0-based index position of its last occurrence, or -1 if not found.

## Syntax

```
${lastindexof;<value>;<list>[;<list>...]}
```

## Parameters

- `value` - The value to search for
- `list` - One or more comma-separated lists to search in

## Behavior

- Combines all lists into one
- Searches for exact match of value
- Returns 0-based index of last occurrence
- Returns -1 if value not found
- Case-sensitive search

## Examples

Find last occurrence:
```
${lastindexof;blue;red,blue,green,blue}
# Returns: 3
```

Not found:
```
${lastindexof;yellow;red,green,blue}
# Returns: -1
```

Search multiple lists:
```
${lastindexof;target;${list1};${list2}}
```

Check existence:
```
${if;${matches;${lastindexof;test;${list}};-?[0-9]+};found;not-found}
```

## Use Cases

- Finding last occurrence positions
- List searching from end
- Duplicate detection
- Reverse searching
- Finding latest matching element

## Notes

- Returns integer index (0-based)
- Last occurrence only
- Case-sensitive
- Combines multiple lists before searching
- See also: `${indexof}` for first occurrence
- See also: `${findlast}` for strings



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
