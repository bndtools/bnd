---
layout: default
class: Macro
title: reject ';' LIST ';' REGEX
summary: Rejects a list by matching it against a regular expression
---

## Summary

Filter a list to exclude elements that match a regular expression. This macro is an alias for the [filterout](filterout.html) macro.

## Syntax

    ${reject;<list>;<regex>}

## Parameters

- **list**: Comma-separated list of elements to filter
- **regex**: Regular expression pattern to match against

## Behavior

Returns a new list containing only the elements from the input list that do **not** match the specified regular expression pattern.

This macro is functionally identical to [filterout](filterout.html).

## Examples

```
# Reject items starting with "test"
packages = com.example.api, com.example.test, com.example.impl
${reject;${packages};.*\.test}
# Returns: com.example.api,com.example.impl

# Exclude test files
files = Main.java, Test.java, Helper.java, TestHelper.java
${reject;${files};.*Test.*}
# Returns: Main.java,Helper.java

# Exclude internal packages
all-packages = com.company.api, com.company.internal, com.company.util
-exportpackage: ${reject;${all-packages};.*\.internal.*}
# Returns: com.company.api,com.company.util

# Remove snapshot versions
versions = 1.0.0, 2.0.0-SNAPSHOT, 3.0.0
${reject;${versions};.*SNAPSHOT.*}
# Returns: 1.0.0,3.0.0
```

## Use Cases

1. **Package Exclusion**: Exclude internal or test packages from exports
2. **File Filtering**: Remove unwanted files from a list
3. **Dependency Filtering**: Exclude specific dependencies by pattern
4. **Clean-up**: Remove items matching unwanted patterns

## Notes

- This is an alias for the [filterout](filterout.html) macro - they behave identically
- Uses Java regular expression syntax
- Elements are matched in their entirety (use `.*` for partial matching)
- The opposite operation is provided by [select](select.html) or [filter](filter.html)

## Related Macros

- [filterout](filterout.html) - Identical functionality (primary name)
- [select](select.html) / [filter](filter.html) - Include only elements matching regex
- [removeall](removeall.html) - Remove specific elements from a list
- [retainall](retainall.html) - Keep only elements present in another list

<hr />
TODO Needs review - AI Generated content

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
