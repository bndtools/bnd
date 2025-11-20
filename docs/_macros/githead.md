---
layout: default
class: Builder
title: githead
summary: Get the Git commit SHA of the current HEAD
---

## Summary

The `githead` macro returns the SHA-1 hash of the current Git HEAD commit. It searches for a `.git` directory starting from the project base directory and walking up the file hierarchy, then resolves symbolic references to find the actual commit hash.

## Syntax

```
${githead}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Searches for a `.git` directory starting from the project base, going up the directory tree
- Handles both regular Git repositories and Git worktrees
- Reads the `.git/HEAD` file to find the current commit
- Resolves symbolic references (e.g., `ref: refs/heads/main`) to actual commit SHAs
- Handles packed references (when Git has optimized/compressed references)
- Returns the commit SHA in uppercase hexadecimal format
- Returns empty string if no Git repository is found

## Examples

Include Git commit in manifest:
```
Bundle-SCM-Revision: ${githead}
# Result: Bundle-SCM-Revision: 4A3B2C1D0E9F8A7B6C5D4E3F2A1B0C9D8E7F6A5B
```

Create version with Git hash:
```
Implementation-Version: ${version}-${githead}
```

Document build source:
```
Build-From: commit ${githead}
```

Conditional on Git availability:
```
git.revision=${if;${githead};${githead};not-in-git-repo}
```

## Use Cases

- Tracking exact source code version in built artifacts
- Creating reproducible builds with commit information
- Debugging - knowing exactly which commit produced an artifact
- Audit trails for compliance and security
- Continuous integration - linking builds to source commits
- Version identification beyond semantic versioning

## Notes

- Returns empty string if not in a Git repository
- Works with Git worktrees (separate working directories)
- Handles both direct SHA references and symbolic references
- Handles Git's packed-refs optimization
- The returned SHA is in uppercase
- The search walks up the directory tree to find the Git repository
- Useful for embedding in bundle manifests for traceability



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
