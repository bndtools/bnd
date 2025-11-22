---
layout: default
title: 	propertiesname 
class: 	Project
summary: Get the filename of the current properties file
---

## Summary

The `propertiesname` macro returns the filename (without path) of the properties file being processed. This is typically `bnd.bnd` for projects or workspace configuration files.

## Syntax

```
${propertiesname}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns the filename of the current properties file
- Returns empty string if no properties file is set
- Returns only the filename, not the full path

## Examples

Get current file name:
```
Current-File: ${propertiesname}
# Returns: "bnd.bnd" or "my.bndrun", etc.
```

Use in logging:
```
Build-Info: Built from ${propertiesname}
```

Conditional based on file:
```
${if;${equals;${propertiesname};test.bnd};test-mode;normal-mode}
```

## Use Cases

- Identifying which configuration file is being processed
- Logging and debugging
- Conditional configuration based on file name
- Documentation generation
- Build metadata

## Notes

- Returns only the filename, not the directory path
- Returns empty string if no properties file
- Typically "bnd.bnd" for projects
- See also: `${propertiesdir}` for the directory path
- See also: `${basedir}` for the project base directory


<hr />
TODO Needs review - AI Generated content

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
