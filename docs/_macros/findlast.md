---
layout: default
class: Macro
title: findlast ';' VALUE ';' SEARCHED
summary: Find the last occurrence of a substring in a string
---

## Summary

The `findlast` macro returns the index position of the last occurrence of a substring within a target string, or -1 if not found. Searches from the end backwards.

## Syntax

```
${findlast;<substring>;<target>}
```

## Parameters

- `substring` - The substring to find (literal string, not regex)
- `target` - The string to search in

## Behavior

- Returns the index of the last occurrence
- Returns -1 if substring is not found
- Index is 0-based
- Case-sensitive search
- Not a regex pattern match

## Examples

Find last occurrence:
```
${findlast;/;/path/to/file.txt}
# Returns: 8 (position of last /)
```

Not found:
```
${findlast;foo;hello world}
# Returns: -1
```

Extract file extension:
```
${substring;${filename};${findlast;.;${filename}}}
```

Get last path component:
```
${substring;${path};${findlast;/;${path}}}
```

## Use Cases

- Finding last delimiter
- Extracting file extensions
- Getting last path component
- String parsing from end
- Finding last occurrence

## Notes

- Returns integer index (0-based)
- Not a regex search
- Case-sensitive
- Searches backwards from end
- See also: `${find}` for first occurrence
- See also: `${lastindexof}` for lists


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
