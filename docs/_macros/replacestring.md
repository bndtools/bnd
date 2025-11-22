---
layout: default
class: Macro
title: replacestring ';' STRING ';' REGEX (';' STRING )?
summary: Replace parts of a string using regex patterns
---

## Summary

The `replacestring` macro applies regex-based replacement to a single string, supporting back-references for captured groups.

## Syntax

```
${replacestring;<string>;<regex>[;<replacement>]}
```

## Parameters

- `string` - The string to process
- `regex` - Regular expression pattern to match
- `replacement` (optional) - Replacement string with `$1-$9` back-references (default: empty)

## Behavior

- Applies `string.replaceAll(regex, replacement)`
- Supports regex back-references (`$1`, `$2`, etc.)
- Default replacement is empty (removes matches)
- All occurrences are replaced

## Examples

Simple replacement:
```
description: This is, possibly, the best implementation ever!
${replacestring;${description};possibly;definitely}
# Returns: "This is, definitely, the best implementation ever!"
```

Remove pattern:
```
${replacestring;version-1.2.3;version-;}
# Returns: "1.2.3"
```

Using back-references:
```
${replacestring;com.example.impl;com\.(.+)\.impl;$1}
# Returns: "example"
```

Multiple replacements:
```
${replacestring;hello world;[aeiou];*}
# Returns: "h*ll* w*rld"
```

Path normalization:
```
${replacestring;${path};\\;/}
# Replace backslashes with forward slashes
```

## Use Cases

- String transformations
- Pattern-based modifications
- Text cleanup
- Path normalization
- Version string manipulation
- String formatting

## Notes

- Regex uses Java syntax
- All occurrences replaced
- Empty replacement removes text
- For lists, use `${replace}` or `${replacelist}`
- See also: `${replace}` for list processing
- See also: `${subst}` for simple string substitution


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
