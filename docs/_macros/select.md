---
layout: default
class: Macro
title: select ';' LIST ';' REGEX
summary: Selects entries in a list that matching a regular expression
---

## Summary

Filter a list to include only elements that match a regular expression. This macro is an alias for the [filter](filter.html) macro.

## Syntax

    ${select;<list>;<regex>}

## Parameters

- **list**: Comma-separated list of elements to filter
- **regex**: Regular expression pattern to match against

## Behavior

Returns a new list containing only the elements from the input list that match the specified regular expression pattern.

This macro is functionally identical to [filter](filter.html).

## Examples

```
# Select items starting with "com."
packages = com.example.api, org.sample.impl, com.example.util
${select;${packages};com\..*}
# Returns: com.example.api,com.example.util

# Select files with specific extension
files = foo.jar, bar.txt, baz.jar
${select;${files};.*\.jar}
# Returns: foo.jar,baz.jar

# Select numbered items
items = item1, item2, other, item3
${select;${items};item\d+}
# Returns: item1,item2,item3

# Select packages matching pattern
-conditionalpackage: ${select;${packages};com\.company\.internal\..*}
```

## Use Cases

1. **Package Filtering**: Select packages matching a naming pattern
2. **File Selection**: Filter file lists by extension or pattern
3. **Dependency Filtering**: Select specific dependencies from a larger list
4. **Pattern Matching**: Include only items matching a specific format

## Notes

- This is an alias for the [filter](filter.html) macro - they behave identically
- Uses Java regular expression syntax
- Elements are matched in their entirety (use `.*` for partial matching)
- The opposite operation is provided by [reject](reject.html) or [filterout](filterout.html)

## Related Macros

- [filter](filter.html) - Identical functionality (primary name)
- [reject](reject.html) / [filterout](filterout.html) - Exclude elements matching regex
- [removeall](removeall.html) - Remove specific elements from a list
- [retainall](retainall.html) - Keep only elements present in another list

<hr />
TODO Needs review - AI Generated content

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
