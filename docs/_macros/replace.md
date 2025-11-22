---
layout: default
class: Macro
title: replace ';' LIST ';' REGEX (';' STRING (';' STRING)? )?
summary: Replace parts of list elements using regex patterns
---

## Summary

The `replace` macro applies regex-based replacement to each element in a comma-separated list. Uses simple comma splitting (doesn't handle quoted sections).

## Syntax

```
${replace;<list>;<regex>[;<replacement>[;<delimiter>]]}
```

## Parameters

- `list` - Comma-separated list of elements
- `regex` - Regular expression pattern to match
- `replacement` (optional) - Replacement string with `$1-$9` back-references (default: empty)
- `delimiter` (optional) - Output delimiter (default: ",")

## Behavior

- Splits list on commas (simple: `\s*,\s*`)
- Applies `element.replaceAll(regex, replacement)` to each
- Supports regex back-references (`$1`, `$2`, etc.)
- Cannot handle commas within quoted sections
- Returns delimited result

## Examples

Add file extension:
```
impls: foo,bar
${replace;${impls};$;.jar}
# Returns: "foo.jar,bar.jar"
```

Replace pattern:
```
${replace;v1.0,v2.0,v3.0;^v;version-}
# Returns: "version-1.0,version-2.0,version-3.0"
```

Using back-references:
```
${replace;com.example.api,com.example.impl;com\.example\.(.+);$1}
# Returns: "api,impl"
```

Custom delimiter:
```
${replace;a,b,c;$;-suffix;;}
# Returns: "a-suffix;b-suffix;c-suffix"
```

## Use Cases

- Adding prefixes/suffixes to list elements
- Pattern-based transformations
- File extension handling
- List element modification
- Bulk string operations

## Notes

- Simple comma splitting only
- For quoted sections, use `${replacelist}`
- Regex uses Java syntax
- Empty replacement removes matched text
- See also: `${replacelist}` for quoted section support
- See also: `${replacestring}` for single string replacement



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
