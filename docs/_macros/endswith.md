---
layout: default
class: Macro
title: endswith ';' STRING ';' SUFFIX
summary: Check if a string ends with a specific suffix
---

## Summary

The `endswith` macro checks if a given string ends with a specified suffix. If the string ends with the suffix, it returns the string; otherwise, it returns an empty string (which evaluates to false in conditional contexts).

## Syntax

```
${endswith;<string>;<suffix>}
```

## Parameters

- `string` - The string to check
- `suffix` - The suffix to look for at the end of the string

## Behavior

- Compares the end of the string with the suffix
- Returns the original string if it ends with the suffix
- Returns an empty string ("") if it does not end with the suffix
- Case-sensitive comparison
- Empty string return value is treated as "false" in conditional expressions

## Examples

Check if a filename ends with .jar:
```
${endswith;mybundle.jar;.jar}
# Returns: "mybundle.jar"
```

Check if a string ends with specific text:
```
${endswith;com.example.api;.api}
# Returns: "com.example.api"
```

Use in conditional logic:
```
${if;${endswith;${project};.test};test-project;regular-project}
# Checks if project name ends with ".test"
```

Filter files with specific extension:
```
jar.file=${if;${endswith;${@};.jar};${@};}
```

Check version suffix:
```
${if;${endswith;${version};-SNAPSHOT};snapshot;release}
```

## Use Cases

- File extension checking
- String suffix validation
- Conditional configuration based on naming patterns
- Filtering lists of values by suffix
- Validating naming conventions
- Conditional manifest header generation

## Notes

- The comparison is case-sensitive
- Returns the original string (truthy) on match, empty string (falsy) on no match
- Useful in combination with `${if}` macro for conditional logic
- See also: `${startswith}` for checking string prefixes



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
