---
layout: default
class: Processor
title: thisfile
summary: Get the absolute path of the current properties file
---

## Summary

The `thisfile` macro returns the absolute path to the properties file being processed. This provides the full path to the current `.bnd`, `.bndrun`, or other bnd configuration file.

## Syntax

```
${thisfile}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns the absolute path to the current properties file
- Generates an error if no properties file is associated with the processor
- The path uses OS-specific separators

## Examples

Get current file path:
```
Current-Config: ${thisfile}
# Returns: "/path/to/project/bnd.bnd"
```

Use in logging:
```
Processing: ${thisfile}
```

Reference in documentation:
```
# Configuration defined in: ${thisfile}
```

Conditional based on file:
```
${if;${endswith;${thisfile};test.bnd};test-mode;normal-mode}
```

## Use Cases

- Debugging configuration files
- Documenting which file defined settings
- Logging and diagnostics
- File-specific conditional logic
- Build metadata
- Tracing configuration sources

## Notes

- Returns absolute path, not relative
- Generates error if called without a properties file
- Path includes the filename
- See also: `${basedir}` for the project directory
- See also: `${propertiesname}` for just the filename
- See also: `${propertiesdir}` for just the directory



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
