---
layout: default
class: Macro
title: unescape ( ';' STRING )*
summary: Convert escape sequences to their control characters
---

## Summary

The `unescape` macro converts escape sequences in strings to their corresponding control characters. It concatenates all input arguments and processes escape sequences.

## Syntax

```
${unescape;<string>[;<string>...]}
```

## Parameters

- `string` - One or more strings to unescape (concatenated before processing)

## Behavior

- Concatenates all input arguments
- Replaces escape sequences with control characters:
  - `\n` → newline (line feed)
  - `\r` → carriage return
  - `\t` → tab
  - `\b` → backspace
  - `\f` → form feed
- Returns the unescaped string

## Examples

Unescape newline:
```
${unescape;line1\nline2}
# Returns: "line1
# line2"
```

Unescape tab:
```
${unescape;col1\tcol2\tcol3}
# Returns: "col1	col2	col3"
```

Multiple strings:
```
${unescape;first\n;second\n;third}
# Returns: "first
# second
# third"
```

Mixed escapes:
```
${unescape;Name:\tJohn\nAge:\t30}
```

## Use Cases

- Creating multi-line strings
- Generating formatted output
- Processing escape sequences in templates
- Building tab-delimited data
- Creating properly formatted text
- Template processing

## Notes

- Only specific escape sequences are supported
- Other backslash sequences remain unchanged
- All arguments are concatenated before unescaping
- Useful for creating formatted text in properties
- Control characters may not display properly in all contexts

<hr />
TODO Needs review - AI Generated content

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
