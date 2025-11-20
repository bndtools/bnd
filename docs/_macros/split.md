---
layout: default
class: Macro
title: split ';' REGEX (';' STRING )*
summary: Split strings into a list using a regular expression
---

## Summary

The `split` macro splits one or more strings into a list using a regular expression as the delimiter. Empty segments are filtered out.

## Syntax

```
${split;<regex>[;<string>...]}
```

## Parameters

- `regex` - Regular expression pattern to split on
- `string` - One or more strings to split

## Behavior

- Splits each string using the regex pattern
- Filters out empty segments
- Combines results from all strings
- Returns comma-separated list

## Examples

Split on comma:
```
${split;,;red,green,blue}
# Returns: "red,green,blue"
```

Split on whitespace:
```
${split;\s+;one two  three   four}
# Returns: "one,two,three,four"
```

Split on pipe:
```
${split;\|;apple|banana|cherry}
# Returns: "apple,banana,cherry"
```

Split multiple strings:
```
${split;:;path1:path2;path3:path4}
# Returns: "path1,path2,path3,path4"
```

Split path on separator:
```
${split;${pathseparator};${some.path}}
```

## Use Cases

- Parsing delimited strings
- Breaking paths into components
- Processing formatted data
- Converting between delimiter formats
- Extracting list elements from strings

## Notes

- Uses Java regex syntax
- Empty segments are automatically filtered
- Multiple input strings are processed
- See also: `${join}` and `${sjoin}` for combining
- See also: `${filter}` for pattern-based filtering



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
