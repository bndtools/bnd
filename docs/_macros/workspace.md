---
layout: default
title: workspace
summary: Get the absolute path to the workspace directory
class: Workspace
---

## Summary

The `workspace` macro returns the absolute file path to the current bnd workspace directory.

## Syntax

```
${workspace}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns absolute path to workspace root
- The workspace is the directory containing `cnf/`
- Path uses OS-specific separators

## Examples

Get workspace path:
```
Workspace-Location: ${workspace}
```

Reference workspace files:
```
config.file=${workspace}/cnf/build.bnd
```

Relative to workspace:
```
${if;${startswith;${basedir};${workspace}};in-workspace;external}
```

## Use Cases

- Referencing workspace-level files
- Configuration paths
- Build metadata
- Multi-project coordination
- Workspace-relative paths

## Notes

- Returns absolute path
- Points to workspace root (contains `cnf/`)
- See also: `${basedir}` for project directory
- See also: `${propertiesdir}` for config file directory




---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
