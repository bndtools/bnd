---
layout: default
title: 	propertiesdir 
class: 	Processor
summary: Get the directory containing the current properties file
---

## Summary

The `propertiesdir` macro returns the absolute path to the directory containing the current properties file being processed.

## Syntax

```
${propertiesdir}
```

## Parameters

None - this macro takes no parameters and will error if arguments are provided.

## Behavior

- Returns absolute path to the directory containing the properties file
- Returns empty string if no properties file is set
- Generates error if called with arguments

## Examples

Get properties directory:
```
Config-Directory: ${propertiesdir}
```

Reference sibling files:
```
license.file=${propertiesdir}/LICENSE.txt
```

Relative resources:
```
resources=${propertiesdir}/resources
```

## Use Cases

- Referencing files relative to config
- Loading resources near bnd files
- Path construction
- Configuration organization
- Related file discovery

## Notes

- Returns absolute path to directory only
- Returns empty string if no properties file
- Cannot take arguments
- See also: `${propertiesname}` for filename
- See also: `${thisfile}` for full path
- See also: `${basedir}` for project root

<hr />
TODO Needs review - AI Generated content

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
