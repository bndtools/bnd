---
layout: default
class: Macro
title: indexof ';' STRING (';' LIST )*
summary: Find the index position of a value in one or more lists
---

## Summary

The `indexof` macro searches for a value in combined lists and returns its 0-based index position, or -1 if not found.

## Syntax

```
${indexof;<value>;<list>[;<list>...]}
```

## Parameters

- `value` - The value to search for
- `list` - One or more comma-separated lists to search in

## Behavior

- Combines all lists into one
- Searches for exact match of value
- Returns 0-based index of first occurrence
- Returns -1 if value not found
- Case-sensitive search

## Examples

Find value in list:
```
${indexof;green;red,green,blue}
# Returns: 1
```

Not found:
```
${indexof;yellow;red,green,blue}
# Returns: -1
```

Search multiple lists:
```
${indexof;target;${list1};${list2}}
```

Check if value exists:
```
${if;${matches;${indexof;test;${list}};-?[0-9]+};found;not-found}
```

## Use Cases

- Finding element positions
- Checking list membership
- List searching operations
- Conditional logic based on presence
- Array indexing

## Notes

- Returns integer index (0-based)
- First occurrence only
- Case-sensitive
- Combines multiple lists before searching
- See also: `${lastindexof}` for last occurrence
- See also: `${find}` for substring search in strings

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
