---
layout: default
class: Macro
title: removeall ';' LIST ';' LIST
summary: Return the first list where items from the second list are removed
---

## Summary

Remove all elements from the first list that are present in the second list, returning the filtered result.

## Syntax

    ${removeall;<list1>[;<list2>]}

## Parameters

- **list1**: Source list from which elements will be removed
- **list2**: (Optional) List of elements to remove from list1

## Behavior

- If only one list is provided, returns an empty string
- If two lists are provided, returns list1 with all elements from list2 removed
- Performs exact string matching (not pattern matching)
- Order of remaining elements is preserved from list1

## Examples

```
# Remove specific packages
all-packages = com.example.api, com.example.impl, com.example.test
test-packages = com.example.test
production-packages = ${removeall;${all-packages};${test-packages}}
# Returns: com.example.api,com.example.impl

# Remove dependencies
all-deps = foo.jar, bar.jar, baz.jar, test.jar
test-deps = test.jar
runtime-deps = ${removeall;${all-deps};${test-deps}}
# Returns: foo.jar,bar.jar,baz.jar

# Remove multiple items
items = a, b, c, d, e
to-remove = b, d
result = ${removeall;${items};${to-remove}}
# Returns: a,c,e

# No second list - returns empty
${removeall;foo,bar,baz}
# Returns: (empty)

# Remove from build path
full-buildpath = ${buildpath}, ${testpath}
exclude-items = junit, mockito
-buildpath: ${removeall;${full-buildpath};${exclude-items}}
```

## Use Cases

1. **Package Exclusion**: Remove specific packages from export or import lists
2. **Dependency Management**: Exclude certain dependencies from build paths
3. **List Filtering**: Remove known unwanted elements from a list
4. **Configuration**: Filter out test or development-only items from production builds

## Notes

- Uses exact string matching, not regular expressions
- For pattern-based filtering, use [reject](reject.html) or [filterout](filterout.html)
- Elements are compared as complete strings
- If an element in list2 doesn't exist in list1, it's simply ignored
- Returns empty string if only one argument is provided

## Related Macros

- [retainall](retainall.html) - Keep only elements present in another list (opposite operation)
- [reject](reject.html) / [filterout](filterout.html) - Remove elements matching a regex pattern
- [filter](filter.html) / [select](select.html) - Keep elements matching a regex pattern
- [uniq](uniq.html) - Remove duplicate elements



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
