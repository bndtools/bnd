---
layout: default
class: Processor
title: basedir
summary: Get the base directory of the current processor context
---

## Summary

The `basedir` macro returns the absolute path to the base directory of the current processor (project, workspace, or build context). This is the root directory from which relative paths are resolved.

## Syntax

```
${basedir}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns the absolute path to the base directory
- The base directory is the root context for resolving relative file paths
- Throws an exception if no base directory has been set (rare edge case)

## Examples

Get the current project's base directory:
```
project.root=${basedir}
```

Build a path relative to the base directory:
```
resources.dir=${basedir}/src/main/resources
```

Reference files using the base directory:
```
Bundle-Icon: ${basedir}/icon.png
```

## Use Cases

- Building absolute paths to project resources
- Referencing files in build configurations
- Creating platform-independent path references
- Debugging path resolution issues
- Documentation and logging of project locations

## Notes

- The base directory is typically the project directory for bnd projects
- In workspace contexts, it may be the workspace root
- The path separator used in the returned path is platform-specific



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
