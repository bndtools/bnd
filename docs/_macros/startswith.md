---
layout: default
class: Macro
title: startswith ';' STRING ';' PREFIX
summary: Check if a string starts with a specific prefix
---

## Summary

The `startswith` macro checks if a given string starts with a specified prefix. Returns the string if it matches, empty string otherwise.

## Syntax

```
${startswith;<string>;<prefix>}
```

## Parameters

- `string` - The string to check
- `prefix` - The prefix to look for

## Behavior

- Checks if string starts with the prefix
- Returns the original string if it starts with prefix
- Returns empty string ("") if it doesn't
- Case-sensitive comparison

## Examples

Check string prefix:
```
${startswith;com.example.api;com.example}
# Returns: "com.example.api"
```

Check with non-matching prefix:
```
${startswith;org.other.package;com.example}
# Returns: ""
```

Use in conditional:
```
${if;${startswith;${project};test.};test-project;regular-project}
```

Filter list by prefix:
```
${filter;${packages};${startswith;.*;com\.example}}
```

## Use Cases

- String prefix validation
- Package name filtering
- Path matching
- Conditional logic based on prefixes
- Name pattern checking

## Notes

- Case-sensitive comparison
- Returns truthy (string) or falsy (empty) value
- Useful with `${if}` for conditional logic
- See also: `${endswith}` for suffix checking
- See also: `${matches}` for regex matching


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
