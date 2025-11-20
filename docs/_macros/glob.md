---
layout: default
class: Macro
title: glob ';' GLOBEXP
summary: Convert a glob pattern to a regular expression
---

## Summary

The `glob` macro converts a glob pattern (shell-style wildcard pattern) into a Java regular expression. This is useful when you need regex patterns but want to write simpler glob syntax.

## Syntax

```
${glob;<glob-pattern>}
```

## Parameters

- `glob-pattern` - A glob pattern using wildcard syntax (e.g., `*.jar`, `com/example/**/*.class`)

## Behavior

- Converts glob wildcards to regex equivalents:
  - `*` → matches any characters except path separator
  - `**` → matches any characters including path separators
  - `?` → matches any single character
  - `[abc]` → matches any character in the set
  - `{a,b,c}` → matches any of the alternatives
- Supports negation with `!` prefix
- Returns a valid Java regular expression string

## Examples

Convert simple wildcard:
```
${glob;*.jar}
# Returns regex matching JAR files
```

Match with path:
```
${glob;com/example/**/*.class}
# Returns regex for any .class file under com/example/
```

Use in filter:
```
${filter;${packages};${glob;com.example.*}}
# Filter packages matching glob pattern
```

Negated pattern:
```
${glob;!*.test.jar}
# Returns negative lookahead regex excluding test JARs
```

Multiple alternatives:
```
${glob;*.{jar,war,ear}}
# Matches files with .jar, .war, or .ear extensions
```

## Use Cases

- Converting user-friendly glob patterns to regex
- Pattern matching in file filtering
- Configuration patterns that are easier to write as globs
- Combining glob syntax with regex-based macros
- Path matching in resource selection
- Building dynamic filter patterns

## Notes

- Glob syntax is simpler and more intuitive than regex for basic patterns
- The `!` prefix creates a negative lookahead pattern
- `*` does not cross directory boundaries (use `**` for that)
- The returned value is a regex pattern string, not a compiled Pattern object
- Glob patterns are case-sensitive by default
- See also: Instructions and filters in bnd often accept glob patterns directly



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
