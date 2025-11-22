---
layout: default
class: Macro
title: literal ';' STRING
summary: Prevent macro expansion by wrapping a value with macro delimiters
---

## Summary

The `literal` macro prevents macro expansion by wrapping a value with the macro prefix `${` and suffix `}`. This effectively creates a literal macro reference that won't be expanded in the current pass.

## Syntax

```
${literal;<value>}
```

## Parameters

- `value` - The string to wrap with macro delimiters

## Behavior

- Wraps the input value with `${` and `}`
- Returns `${<value>}`
- Prevents immediate macro expansion
- The result can be expanded in a later processing pass

## Examples

Create a literal macro reference:
```
${literal;version}
# Returns: "${version}"
```

Delay macro expansion:
```
deferred=${literal;basedir}
# deferred contains the string "${basedir}", not the actual path
```

Pass macro syntax as a value:
```
template=${literal;project.name}
# Later: expanded=${template} will evaluate ${project.name}
```

Store macro patterns:
```
pattern=${literal;if;condition;true;false}
# Returns: "${if;condition;true;false}"
```

## Use Cases

- Delaying macro expansion to a later processing stage
- Storing macro patterns as values
- Generating dynamic macro references
- Template creation and processing
- Multi-pass macro processing
- Escaping macro syntax

## Notes

- The result is a string, not an expanded macro
- Useful for meta-programming with macros
- The wrapped value will be expanded if processed again
- Does not escape inner content - only wraps it
- Requires exactly one argument
- Returns a literal macro syntax string
- See also: Property substitution and template processing



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
