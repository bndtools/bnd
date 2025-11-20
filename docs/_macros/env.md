---
layout: default
class: Macro
title: env ';' KEY (';' STRING)?
summary: Get an environment variable with an optional default
---

## Summary

The `env` macro looks up an environment variable and returns its value, or returns a default value if the variable is not set. The default is an empty string if not specified.

## Syntax

```
${env;<variable>[;<default>]}
```

## Parameters

- `variable` - The environment variable name to look up
- `default` (optional) - Value to return if variable not set (default: empty string)

## Behavior

- Looks up environment variable via `System.getenv()`
- Returns variable value if set
- Returns default if variable not set
- Default is empty string if not specified
- Case-sensitive on Unix/Linux, case-insensitive on Windows

## Examples

Get environment variable with default:
```
${env;JAVA_HOME;/usr/lib/jvm/default}
```

Check if variable exists:
```
${if;${env;CI};running-in-ci;local-build}
```

Use in paths:
```
user.home=${env;HOME;/home/default}
```

Empty default:
```
${env;OPTIONAL_VAR}
# Returns value or "" if not set
```

## Use Cases

- Accessing environment configuration
- CI/CD environment detection
- User-specific paths
- System configuration
- Platform differences
- Secure credentials (with caution)

## Notes

- Accesses system environment variables
- Case sensitivity varies by OS
- Returns default for undefined variables
- Empty string is valid default
- **Security**: Be cautious with credentials
- See also: `${def}` for properties
- See also: `${system}` for system commands 


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
