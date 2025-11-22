---
layout: default
class: Workspace
title: global ';' KEY ( ';' DEFAULT )? 
summary: Access user settings from the ~/.bnd/settings.json file
---

## Summary

The `global` macro provides access to user-specific settings stored in the `~/.bnd/settings.json` file. This allows builds to use machine-specific configuration like credentials, API tokens, or user preferences without storing them in project files.

## Syntax

```
${global;<key>[;<default>]}
```

## Parameters

- `key` - The setting name to retrieve from settings.json
- `default` (optional) - Default value to return if the setting is not found

## Behavior

- Reads from the user's `~/.bnd/settings.json` file in their home directory
- Returns the value for the specified key
- Returns the default value if provided and key is not found
- Returns null if key is not found and no default is provided
- Special keys:
  - `key.public` - Returns the user's public key as hex string
  - `key.private` - Returns the user's private key as hex string

## Examples

Access a custom setting:
```
maven.user=${global;maven.username}
```

Use with default value:
```
api.key=${global;example.api.key;default-key}
```

Access credentials:
```
-connection: ${global;nexus.url}; \
             uid=${global;nexus.username}; \
             pwd=${global;nexus.password}
```

Get public key:
```
signing.key=${global;key.public}
```

Conditional on global setting:
```
${if;${global;enable.feature};enabled;disabled}
```

## Use Cases

- Storing user-specific credentials outside project files
- Machine-specific configuration (different per developer)
- API tokens and authentication information
- User preferences for build behavior
- Private repository credentials
- Signing keys and certificates
- Environment-specific URLs and endpoints

## Notes

- Settings are stored in JSON format in `~/.bnd/settings.json`
- The settings file is per-user, not per-project
- Keeps sensitive information out of version control
- Returns null (not empty string) when key is missing and no default
- The special `key.public` and `key.private` keys access cryptographic keys
- Settings can be edited directly in the JSON file
- Useful for CI/CD where different machines need different configurations



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
