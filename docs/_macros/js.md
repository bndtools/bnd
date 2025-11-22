---
layout: default
class: Macro
title: js (';' JAVASCRIPT )*
summary: Execute JavaScript expressions and return the result. 
---

## Summary

Deprecated: Javascript script engine removed in Java 15. This macro might not work anymore and might be removed in future versions.

The `js` macro executes one or more JavaScript expressions using the Java ScriptEngine and returns the value of the last expression or any output produced.

## Syntax

```
${js;<expression>[;<expression>...]}
```

## Parameters

- `expression` - One or more JavaScript expressions to execute

## Behavior

- Executes all expressions in sequence
- Returns the value of the last expression
- If no return value, returns stdout output
- Has access to `domain` object (Processor instance)
- Persistent context across macro invocations
- Can load JavaScript from `javascript` property

## Examples

Simple calculation:
```
${js;2 + 2}
# Returns: "4"
```

String manipulation:
```
${js;"hello".toUpperCase()}
# Returns: "HELLO"
```

Multiple expressions:
```
${js;var x = 10;x * 2}
# Returns: "20"
```

Access properties:
```
${js;domain.getProperty("version")}
```

## Use Cases

- Complex calculations
- String transformations
- Conditional logic too complex for macros
- Property manipulation
- Dynamic value generation

## Notes

- Uses Java's ScriptEngine (Nashorn/GraalVM)
- Context persists between calls
- `domain` object provides access to bnd
- JavaScript property can define initialization code
- Errors reported to build
- **Warning**: Adds JavaScript engine dependency



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
