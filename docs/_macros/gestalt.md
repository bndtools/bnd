---
layout: default
class: Workspace
title: gestalt ';' NAME ( ';' NAME (';' ANY )? )? 
summary: Access environment description properties (gestalt)
---

## Summary

The `gestalt` macro provides access to gestalt properties that describe the build environment. Gestalt is a set of parts describing the environment, where each part has a name and optional attributes. This can be configured globally or per-workspace via the `-gestalt` property.

## Syntax

```
${gestalt;<part>[;<key>[;<value>]]}
```

## Parameters

- `part` - The gestalt part name
- `key` (optional) - Attribute key within the part
- `value` (optional) - Expected value to match

## Behavior

- **One argument** (`${gestalt;<part>}`): Returns the part name if it exists, empty otherwise
- **Two arguments** (`${gestalt;<part>;<key>}`): Returns the attribute value for the key, or empty if not found
- **Three arguments** (`${gestalt;<part>;<key>;<value>}`): Returns the value if it matches, empty otherwise (useful for boolean checks)

## Examples

Check if gestalt part exists:
```
${gestalt;ci}
# Returns: "ci" if CI gestalt is defined, "" otherwise
```

Get attribute value:
```
${gestalt;platform;os}
# Returns the OS attribute value from platform gestalt
```

Match specific value:
```
${if;${gestalt;ci;provider;github};github-ci;other-ci}
```

Configure gestalt:
```
-gestalt: ci;provider=github, platform;os=linux
```

Use in conditional logic:
```
${if;${gestalt;ci};ci-mode;local-mode}
```

## Use Cases

- Environment detection and adaptation
- Build behavior customization per environment
- CI/CD-specific configuration
- Platform-specific settings
- Feature flags based on environment
- Conditional build logic

## Notes

- Gestalt parts can be added programmatically or via `-gestalt` property
- Global gestalt affects all workspaces
- Per-workspace gestalt augments global gestalt
- Empty string return values can be used in conditionals (falsy)
- Useful with `${if}` macro for environment-specific behavior



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
