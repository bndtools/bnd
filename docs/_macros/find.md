---
layout: default
class: Macro
title: find ';' VALUE ';' SEARCHED
summary: Find the index position of a substring in a string
---

## Summary

The `find` macro returns the starting index position of a substring within a target string, or -1 if not found. This is a simple string search, not a regex pattern match.

## Syntax

```
${find;<target>;<substring>}
```

## Parameters

- `target` - The string to search in
- `substring` - The substring to find (literal string, not regex)

## Behavior

- Returns the index of the first occurrence of substring
- Returns -1 if substring is not found
- Index is 0-based (first character is position 0)
- Case-sensitive search
- Not a regex pattern match

## Examples

Find substring position:
```
${find;hello world;world}
# Returns: 6
```

Search not found:
```
${find;hello world;foo}
# Returns: -1
```

Check if string contains substring:
```
${if;${matches;${find;${path};/test/};-?[0-9]+};contains-test;no-test}
```

Find file extension:
```
${find;${filename};.}
```

## Use Cases

- Locating substring positions
- Checking if string contains text
- String parsing and analysis
- Finding delimiters
- Conditional logic based on presence

## Notes

- Returns integer index (0-based)
- Not a regex search (use `${matches}` for regex)
- Case-sensitive
- Returns first occurrence only
- See also: `${lastindexof}` for last occurrence
- See also: `${matches}` for regex matching
- See also: `${indexof}` for list searching



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
