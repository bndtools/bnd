---
layout: default
class: Macro
title: replacelist ';' LIST ';' REGEX (';' STRING (';' STRING)? )?
summary: Replace parts of list elements using regex with quoted section support
---

## Summary

The `replacelist` macro applies regex-based replacement to each element in a list. Unlike `${replace}`, it uses a sophisticated splitter that handles quoted sections, preserving commas within quotes.

## Syntax

```
${replacelist;<list>;<regex>[;<replacement>[;<delimiter>]]}
```

## Parameters

- `list` - List of elements (can contain quoted sections with commas)
- `regex` - Regular expression pattern to match
- `replacement` (optional) - Replacement string with `$1-$9` back-references (default: empty)
- `delimiter` (optional) - Output delimiter (default: ",")

## Behavior

- Splits list intelligently (respects quoted sections)
- Applies `element.replaceAll(regex, replacement)` to each
- Supports regex back-references (`$1`, `$2`, etc.)
- Preserves quoted sections during splitting
- Returns delimited result

## Examples

Add to quoted elements:
```
impls: foo;version="[1,2)", bar;version="[1.2,2)"
${replacelist;${list;impls};$;\\;strategy=highest}
# Returns: foo;version="[1,2)";strategy=highest,bar;version="[1.2,2)";strategy=highest
```

Process dependencies:
```
deps: lib;version="1.0,2.0",tool;version="2.0"
${replacelist;${deps};$;,optional}
```

Back-references with quotes:
```
${replacelist;a="x,y",b="z";(.+)="(.+)";$1:$2}
```

## Use Cases

- OSGi manifest attribute manipulation
- Dependency list processing
- Version range handling
- Complex list transformations
- Preserving structured data

## Notes

- Handles quoted sections properly
- More robust than `${replace}` for OSGi attributes
- Regex uses Java syntax
- Empty replacement removes matched text
- See also: `${replace}` for simple splitting
- See also: `${replacestring}` for single strings


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
