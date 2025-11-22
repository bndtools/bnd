---
layout: default
class: Macro
title: retainall ';' LIST ';' LIST
summary: Return the first list where items not in the second list are removed
---

## Summary

Keep only elements from the first list that are also present in the second list, effectively computing the intersection of two lists.

## Syntax

    ${retainall;<list1>;<list2>}

## Parameters

- **list1**: Source list from which elements will be kept
- **list2**: List of elements to retain from list1

## Behavior

- Returns a new list containing only elements that appear in both lists
- Performs exact string matching (not pattern matching)
- Order of elements is preserved from list1
- Returns empty string if fewer than two lists are provided
- Acts as a set intersection operation

## Examples

```
# Find common packages
packages1 = com.example.api, com.example.impl, com.example.util
packages2 = com.example.api, com.example.util, com.other.api
common-packages = ${retainall;${packages1};${packages2}}
# Returns: com.example.api,com.example.util

# Keep only approved dependencies
all-deps = foo.jar, bar.jar, baz.jar, qux.jar
approved-deps = foo.jar, baz.jar
used-deps = ${retainall;${all-deps};${approved-deps}}
# Returns: foo.jar,baz.jar

# List intersection
list-a = a, b, c, d
list-b = c, d, e, f
intersection = ${retainall;${list-a};${list-b}}
# Returns: c,d

# Filter build path to allowed items
buildpath = ${workspace.buildpath}
allowed-libs = commons-io, commons-lang, slf4j-api
-buildpath: ${retainall;${buildpath};${allowed-libs}}

# No common elements
list-x = a, b, c
list-y = d, e, f
${retainall;${list-x};${list-y}}
# Returns: (empty)
```

## Use Cases

1. **Whitelist Filtering**: Keep only approved/allowed elements from a list
2. **Set Intersection**: Find common elements between two lists
3. **Dependency Validation**: Ensure only permitted dependencies are used
4. **Package Filtering**: Keep only packages that meet certain criteria
5. **Configuration Merging**: Identify common configuration items

## Notes

- Uses exact string matching, not regular expressions
- For pattern-based filtering, use [filter](filter.html) or [select](select.html)
- Elements are compared as complete strings
- Returns empty string if fewer than two arguments provided
- This is the opposite of [removeall](removeall.html) - retainall keeps matching elements, removeall removes them
- Order of retained elements follows list1's order

## Related Macros

- [removeall](removeall.html) - Remove elements present in another list (opposite operation)
- [filter](filter.html) / [select](select.html) - Keep elements matching a regex pattern
- [reject](reject.html) / [filterout](filterout.html) - Remove elements matching a regex pattern
- [uniq](uniq.html) - Remove duplicate elements



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
