---
layout: default
class: Macro
title: apply ';' MACRO (';' LIST)* 
summary: Convert a list to an invocation with arguments 
---

## Summary

Convert comma-separated lists into semicolon-separated macro arguments, effectively "applying" list elements as individual macro parameters.

## Syntax

    ${apply;<macro-name>[;<list>...]}

## Parameters

- **macro-name**: Name of the macro to invoke (without `${}`)
- **list...**: Zero or more comma-separated lists whose elements become macro arguments

## Behavior

The macro:
1. Takes the specified macro name
2. Splits all provided lists into individual elements (using comma separation)
3. Constructs a new macro invocation with elements as semicolon-separated arguments
4. Expands the constructed macro

This is useful when you have arguments in a comma-separated list but need to pass them as separate parameters to a macro.

## Examples

```
# Basic example - convert list to macro arguments
args = com.example.foo, 3.12, HIGHEST
${apply;repo;${args}}
# Expands to: ${repo;com.example.foo;3.12;HIGHEST}

# Apply multiple lists
list1 = a, b
list2 = c, d
${apply;mymacro;${list1};${list2}}
# Expands to: ${mymacro;a;b;c;d}

# Use with filter macro
packages = com.example.*, org.sample.*, com.test.*
pattern = com\..*
${apply;filter;${packages};${pattern}}
# Expands to: ${filter;com.example.*,org.sample.*,com.test.*;com\..*}

# Repository lookup with dynamic arguments
repo-args = my.bundle, 1.0.0, HIGHEST
bundle-path = ${apply;repo;${repo-args}}

# Use with format macro
format-string = %s-%s-%s
values = group, artifact, version
${apply;format;${format-string};${values}}
# Expands to: ${format;%s-%s-%s;group;artifact;version}

# Complex example with multiple lists
deps1 = foo, bar
deps2 = baz
${apply;join;,;${deps1};${deps2}}
# Expands to: ${join;,;foo;bar;baz}
```

## Use Cases

1. **Dynamic Macro Calls**: Build macro invocations from list-based configuration
2. **Repository Queries**: Pass artifact coordinates as a list
3. **Parameter Transformation**: Convert comma-separated to semicolon-separated
4. **Configuration Flexibility**: Store macro arguments in properties as lists
5. **Macro Composition**: Combine multiple lists into a single macro call

## Notes

- The macro name is specified without `${}` delimiters
- Lists are split on commas (respects quoted strings)
- All list elements become separate arguments to the target macro
- The resulting macro is immediately expanded
- Multiple lists are concatenated in order
- Empty lists contribute no arguments

## Related Macros

- [template](template.html) - More sophisticated list-to-macro transformation
- [foreach](foreach.html) - Iterate over list elements
- [join](join.html) - Join list elements with a delimiter
- [list](list.html) - Merge property lists

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
