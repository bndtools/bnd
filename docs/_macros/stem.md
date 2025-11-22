---
layout: default
class: Macro
title: stem ';' STRING 
summary: Extract the portion of a string before the first dot
---

## Summary

The `stem` macro returns the portion of a string up to (but not including) the first dot character. If no dot is found, returns the entire string.

## Syntax

```
${stem;<string>}
```

## Parameters

- `string` - The string to process

## Behavior

- Finds the first dot (`.`) character
- Returns substring before the first dot
- Returns entire string if no dot is found
- Only considers the first dot (not subsequent dots)

## Examples

Extract file basename:
```
${stem;example.txt}
# Returns: "example"
```

Get package prefix:
```
${stem;com.example.api}
# Returns: "com"
```

Extract version major:
```
${stem;1.2.3}
# Returns: "1"
```

No dot present:
```
${stem;filename}
# Returns: "filename"
```

Multiple dots:
```
${stem;archive.tar.gz}
# Returns: "archive" (only first dot)
```

## Use Cases

- Extracting file basenames without extension
- Getting first component of qualified names
- Parsing dotted strings
- Version number parsing
- File name manipulation
- Package name prefixes

## Notes

- Only removes up to the first dot
- For removing all extensions, use multiple applications or other methods
- Returns original string if no dot found
- Empty string before dot returns empty
- See also: `${basename}` for file name extraction



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
