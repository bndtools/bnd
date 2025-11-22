---
layout: default
class: Macro
title: warning ( ';' STRING )*
summary: Generate a build warning with a custom message
---

## Summary

The `warning` macro generates one or more build warnings with custom messages. Each message argument is processed (macros are expanded) and then added to the build warning list. Unlike errors, warnings do not fail the build.

## Syntax

```
${warning;<message>[;<message>...]}
```

## Parameters

- `message` - One or more warning messages to generate. Each message can contain macros that will be expanded.

## Behavior

- Processes each message argument (expanding any macros within)
- Adds each processed message to the build warning list
- Returns an empty string
- Does not fail the build (unlike `${error}`)
- Warning location is tracked for better diagnostics

## Examples

Generate a simple warning:
```
${warning;This configuration is deprecated}
```

Generate warning with variable substitution:
```
${warning;Using default version: ${version}}
```

Conditional warning:
```
${if;${is;${someproperty}};${warning;Property someproperty is set and may cause issues}}
```

Multiple warning messages:
```
${warning;First warning;Second warning;Third warning}
```

Warning with computed values:
```
${warning;Bundle ${bsn} is using experimental features}
```

## Use Cases

- Alerting to deprecated configurations
- Notifying about suboptimal settings
- Flagging potential issues without failing
- Providing informational messages
- Conditional notifications
- Configuration validation feedback

## Notes

- Warnings are visible but don't fail the build
- Each argument becomes a separate warning
- Macros within warning messages are expanded
- Use `${error}` to fail the build instead
- Warnings are tracked with file and line location
- Empty return value allows use in property assignments



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
