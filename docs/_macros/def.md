---
layout: default
class: Macro
title: def ';' KEY (';' STRING)?
summary: Get a property value with an optional default
---

## Summary

The `def` macro looks up a property by key and returns its value, or returns a default value if the property is not set. The default is an empty string if not specified.

## Syntax

```
${def;<key>[;<default>]}
```

## Parameters

- `key` - The property name to look up
- `default` (optional) - Value to return if property not set (default: empty string)

## Behavior

- Looks up property by key
- Returns property value if set
- Returns default if property not set
- Default is empty string if not specified
- Handles undefined properties gracefully

## Examples

Get property with default:
```
${def;version;1.0.0}
# Returns property value or "1.0.0" if not set
```

Empty default:
```
${def;optional.setting}
# Returns property value or "" if not set
```

Use in paths:
```
output.dir=${def;custom.dir;target}
```

Conditional with default:
```
${if;${def;debug.mode;false};debug-on;debug-off}
```

## Use Cases

- Providing default values
- Optional properties
- Configuration fallbacks
- Safe property access
- Avoiding undefined property errors

## Notes

- Safer than direct property reference
- Default prevents empty/undefined issues
- Unlike `${key}`, returns default instead of literal
- Empty string is valid default
- See also: `${get}` for list element access
- See also: `${if}` for conditional defaults 


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
