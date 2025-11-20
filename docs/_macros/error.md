---
layout: default
class: Macro
title: error ( ';' STRING )*
summary: Generate a build error with a custom message
---

## Summary

The `error` macro generates one or more build errors with custom messages. Each message argument is processed (macros are expanded) and then added to the build error list. This causes the build to fail.

## Syntax

```
${error;<message>[;<message>...]}
```

## Parameters

- `message` - One or more error messages to generate. Each message can contain macros that will be expanded.

## Behavior

- Processes each message argument (expanding any macros within)
- Adds each processed message to the build error list
- Returns an empty string
- Causes the build to fail
- Error location is tracked for better diagnostics

## Examples

Generate a simple error:
```
${error;This configuration is not supported}
```

Generate error with variable substitution:
```
${error;Invalid version: ${version}}
```

Conditional error:
```
${if;${is;${someproperty}};${error;Property someproperty must not be set}}
```

Multiple error messages:
```
${error;First error message;Second error message;Third error message}
```

Error with computed values:
```
${error;Bundle ${bsn} requires Java ${ee} but ${java.version} is configured}
```

## Use Cases

- Enforcing build-time constraints
- Validating configuration values
- Preventing invalid bundle configurations
- Providing clear error messages for misconfiguration
- Failing fast when prerequisites are not met
- Custom validation rules in build files

## Notes

- The build will fail when an error is generated
- Each argument becomes a separate error message
- Macros within error messages are expanded before display
- Use `${warning}` instead if you want to generate warnings without failing the build
- Errors are tracked with file and line location for debugging
- Empty return value allows use in property assignments without side effects




---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
