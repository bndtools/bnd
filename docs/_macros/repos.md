---
layout: default
title: repos
summary: Get a list of configured repository names
class: Project
---

## Summary

The `repos` macro returns a comma-separated list of the names of all repositories configured in the current project or workspace.

## Syntax

```
${repos}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns names of all configured repository plugins
- Repositories are listed in comma-separated format
- Includes all repository types (Maven, File, OBR, etc.)
- Returns the repository names as configured

## Examples

Get all repository names:
```
${repos}
# Returns: "Maven Central, Local, Workspace, Release" (example)
```

Document available repositories:
```
Bundle-RepositoriesUsed: ${repos}
```

Count repositories:
```
repo.count=${size;${split;,;${repos}}}
```

Check for specific repository:
```
${if;${findlast;${repos};Maven Central};has-central;no-central}
```

## Use Cases

- Listing available repositories in documentation
- Debugging repository configuration
- Validating repository setup
- Build metadata and reporting
- Repository availability checks

## Notes

- Returns repository names, not paths or URLs
- Names are as configured in the workspace or project
- The list reflects the current configuration
- Repository order may vary
- See also: `${repo}` for accessing repository contents
- Repository configuration is in `cnf/build.bnd` or project files



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
