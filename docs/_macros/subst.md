---
layout: default
class: Macro
title: subst ';' STRING ';' REGEX (';' STRING (';' NUMBER )? )?
summary: Substitute all the regex matches in the target for the given value; if a count is specified, limit the number of replacements to that count.
---

## Summary

Perform regex-based substring substitution on a target string, with optional replacement limit.

## Syntax

    ${subst;<target>;<regex>[;<replacement>[;<count>]]}

## Parameters

- **target**: String in which to perform substitutions
- **regex**: Regular expression pattern to match
- **replacement**: (Optional) Replacement string for matches. Defaults to empty string.
- **count**: (Optional) Maximum number of replacements to perform. Defaults to all matches.

## Behavior

The macro:
1. Finds all substrings in `target` that match `regex`
2. Replaces them with `replacement` (or empty string if not specified)
3. Limits replacements to `count` if specified (otherwise replaces all)
4. Returns the modified string

This is similar to Java's `String.replaceAll()` but with optional replacement count.

## Examples

```
# Remove file extension
${subst;foo.bar;\.bar}
# Returns: foo

# Replace dots with dashes
${subst;com.example.package;\.;-}
# Returns: com-example-package

# Replace with specified text
${subst;Hello World;World;Universe}
# Returns: Hello Universe

# Limit replacements
${subst;a,b,c,d;,;-;2}
# Returns: a-b-c,d (only first 2 replacements)

# Remove all digits
${subst;version1.2.3;\d}
# Returns: version.. (empty replacement)

# Replace spaces with underscores
${subst;My Project Name; ;_}
# Returns: My_Project_Name

# Complex regex - remove version info
${subst;bundle-1.0.0.jar;-[\d.]+\.jar;.jar}
# Returns: bundle.jar

# Multiple occurrences
${subst;test-test-test;test;prod}
# Returns: prod-prod-prod

# With capture groups
${subst;com.example.api;com\.(.*)\.api;org.$1.impl}
# Returns: org.example.impl
```

## Use Cases

1. **Path Manipulation**: Modify file paths or package names
2. **String Cleaning**: Remove unwanted patterns from strings
3. **Format Conversion**: Transform string formats
4. **Version Stripping**: Remove version numbers from names
5. **Pattern Replacement**: Replace specific patterns with alternatives

## Notes

- Uses Java regular expression syntax
- Default replacement is empty string (effectively removes matches)
- Default count is unlimited (replaces all matches)
- Replacement string can contain capture group references (`$1`, `$2`, etc.)
- To use literal `$` in replacement, escape it as `$$`
- The count parameter limits the number of replacements, not matches
- For more complex transformations, consider [replacestring](replacestring.html)

## Related Macros

- [replace](replace.html) / [replacestring](replacestring.html) - Replace in strings with patterns
- [replacelist](replacelist.html) - Replace patterns in list elements
- [filter](filter.html) - Filter list elements by regex
- [substring](substring.html) - Extract substring by position
	


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
