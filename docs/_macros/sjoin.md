---
layout: default
class: Macro
title: sjoin ';' SEPARATOR ( ';' LIST )+
summary: Join lists with a custom separator
---

## Summary

The `sjoin` macro combines one or more lists into a single string using a custom separator. Unlike `${join}` which always uses commas, this allows you to specify any separator.

## Syntax

```
${sjoin;<separator>;<list>[;<list>...]}
```

## Parameters

- `separator` - The string to use between elements (can be any string)
- `list` - One or more lists to combine

## Behavior

- Combines all provided lists into a single collection
- Joins elements using the specified separator
- The separator can be any string (space, newline, custom text, etc.)
- Returns the joined string

## Examples

Join with space:
```
${sjoin; ;apple,banana,cherry}
# Returns: "apple banana cherry"
```

Join with custom separator:
```
${sjoin; | ;red,green,blue}
# Returns: "red | green | blue"
```

Join with newline:
```
${sjoin;\n;line1,line2,line3}
# Returns multi-line text
```

Join multiple lists with colon:
```
${sjoin;:;${exports};${imports}}
# Returns packages separated by colons
```

Create formatted list:
```
${sjoin;, and ;first,second,third}
# Returns: "first, and second, and third"
```

## Use Cases

- Creating custom formatted output
- Building strings with specific delimiters
- Generating reports or logs
- Creating paths with custom separators
- Formatting lists for display
- Building command-line arguments

## Notes

- The separator can be any string, including special characters
- Input lists can use comma or semicolon separation
- Empty separator joins elements directly (no space)
- See also: `${join}` for comma-separated joining
- See also: `${path}` for OS-specific path separator joining
	



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
