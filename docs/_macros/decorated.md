---
layout: default
class: Macro
title: decorated ';' NAME [ ';' BOOLEAN ]
summary: The merged and decorated Parameters object
---

## Summary

Get a merged and decorated Parameters object, where decoration allows pattern-based attribute addition to parameter entries.

## Syntax

    ${decorated;<property-name>[;<include-literals>]}

## Parameters

- **property-name**: Name of the property (not its expanded value) whose Parameters should be decorated
- **include-literals**: (Optional) Boolean - whether to include unused literal entries from decorators. Defaults to `false`.

## Behavior

The macro:
1. **Merges** all properties starting with the given name (including `.extra` suffixes)
2. **Decorates** entries by matching them against decorator patterns from properties ending with `+`
3. Returns the merged and decorated Parameters as a string

### Decoration Process

A "decorator" property (name ending with `+`) contains glob patterns as keys:
- Patterns are matched against keys from the merged parameters
- Matching entries receive additional attributes from the decorator
- If `include-literals` is `true`, unmatched decorator entries are also included

## Examples

```
# Basic decoration
parameters = a, b
parameters.extra = c, d
parameters+ = (c|d);attr=X
${decorated;parameters}
# Returns: a,b,c;attr=X,d;attr=X

# Include literals
parameters = a, b
parameters.extra = c, d
parameters+ = (c|d);attr=X, x, y, z
${decorated;parameters;true}
# Returns: a,b,c;attr=X,d;attr=X,x,y,z
# (x,y,z are literals from decorator, included because second arg is true)

# Package decoration
-exportpackage = com.example.api, com.example.util
-exportpackage.internal = com.example.internal
-exportpackage+ = *.internal;version=0.0.0;x-internal:=true
Export-Package: ${decorated;-exportpackage}
# internal package gets version and x-internal attributes

# Build path decoration
-buildpath = commons-io, commons-lang
-buildpath.test = junit, mockito
-buildpath+ = junit;version=4.12, mockito;version=2.0
${decorated;-buildpath}
# Test dependencies get specific versions

# Conditional attributes
packages = com.example.*, org.sample.*
packages+ = com.example.*;export=true
packages+ = org.sample.*;export=false
${decorated;packages}

# Multiple decorators
deps = foo, bar, baz
deps+ = foo;version=1.0
deps+ = bar;version=2.0
${decorated;deps}
# Returns: foo;version=1.0,bar;version=2.0,baz
```

## Use Cases

1. **Package Configuration**: Add version or visibility attributes to packages
2. **Dependency Management**: Apply version constraints to dependencies
3. **Conditional Attributes**: Add different attributes based on patterns
4. **Build Configuration**: Decorate build paths with specific attributes
5. **Manifest Generation**: Create decorated manifest headers

## Notes

- Property name is specified, not the expanded value
- Merges all properties with the same base name (including `.extra` suffixes)
- Decorator properties end with `+` and use glob patterns for matching
- Glob patterns: `*` (any chars), `?` (one char), `[abc]` (char class)
- Multiple decorators can apply to the same entry (attributes merged)
- If `include-literals` is false (default), only matched patterns contribute
- If `include-literals` is true, all decorator entries are included
- See [instructions](/chapters/820-instructions.html) for more on decorated parameters

## Related Macros

- [template](template.html) - Transform Parameters with templates
- [list](list.html) - Merge property lists
- [global](global.html) - Access workspace-level settings


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
