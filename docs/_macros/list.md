---
layout: default
class: Macro
title: list (';' KEY)*
summary: Returns a list of the values of the named properties with escaped semicolons.
---

## Summary

Merge multiple property values into a single comma-separated list with automatic semicolon escaping, useful for combining list-valued properties that contain semicolons.

## Syntax

    ${list[;<property-name>...]}

## Parameters

- **property-name...**: Zero or more property names whose values should be merged

## Behavior

The macro:
1. Looks up each property name to get its value
2. Splits each value using quoted string parsing (respects quotes)
3. Escapes unescaped semicolons within elements (replaces `;` with `\;`)
4. Merges all elements into a single comma-separated list
5. Returns an empty string if no properties are specified

This is particularly useful when list elements contain semicolons (like OSGi version ranges or attributes) and need to be passed to other macros.

## Examples

```
# Define list properties with semicolons
deps = com.foo;version="[1,2)", com.bar;version="[1.2,2)"

# Merge and add attributes to each element
-buildpath: ${replacelist;${list;deps};$;\\;strategy=highest}
# Results in:
# com.foo;version="[1,2)";strategy=highest,com.bar;version="[1.2,2)";strategy=highest

# Merge multiple lists
files = foo.jar, bar.jar
extras = baz.jar, qux.jar
all-files = ${list;files;extras}
# Returns: foo.jar,bar.jar,baz.jar,qux.jar

# Merge build paths with attributes
compile-deps = javax.servlet;version=3.0, commons-io;version=2.5
runtime-deps = slf4j-api;version=1.7
all-deps = ${list;compile-deps;runtime-deps}

# Create a combined package list with versions
Private-Package: ${list;internal-packages;test-packages}

# Handle empty properties (no-op for empty ones)
${list;optional-deps;main-deps}
# Only includes non-empty properties

# Complex example with attributes
base-deps = org.osgi.core;version="[6,7)", org.osgi.service.component;version="[1.3,2)"
extra-deps = org.apache.felix.scr;version="[2,3)"
-buildpath: ${list;base-deps;extra-deps}
```

## Use Cases

1. **Dependency Management**: Combine dependencies with version ranges from different sources
2. **Build Path Merging**: Merge multiple classpath or build path properties
3. **Package Lists**: Combine package lists that include OSGi attributes
4. **Attribute Manipulation**: Prepare lists for processing with `replacelist` to add attributes
5. **Configuration Merging**: Merge configuration lists from different profiles

## Notes

- Properties are specified by name, not by their expanded values
- Empty or undefined properties are simply skipped
- Semicolons within list elements are automatically escaped to `\;`
- Uses comma as the delimiter for the output list
- Respects quoted strings in property values
- Order of elements matches the order of property arguments
- Particularly useful for OSGi manifest headers that use semicolons for attributes

## Related Macros

- [replacelist](replacelist.html) - Apply regex replacements to list elements
- [join](join.html) - Join list elements with a custom delimiter
- [filter](filter.html) - Filter list elements by regex
- [uniq](uniq.html) - Remove duplicate elements from a list
- [sort](sort.html) - Sort list elements



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
