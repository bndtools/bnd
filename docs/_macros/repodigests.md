---
layout: default
title: repodigests ( ';' NAME )*
summary: Get cryptographic digests of repository contents
class: Workspace
---

## Summary

The `repodigests` macro returns hexadecimal cryptographic digests representing the contents of all repositories or specific named repositories. This is useful for detecting changes in repository state.

## Syntax

```
${repodigests[;<repo-name>...]}
```

## Parameters

- `repo-name` (optional) - One or more repository names. If omitted, returns digests for all repositories.

## Behavior

- Calculates digest for each specified repository
- Returns comma-separated hex-encoded digests
- Repositories without digest support are skipped
- Reports error if named repository not found
- Uses repository's `getDigest()` method

## Examples

Get all repository digests:
```
${repodigests}
# Returns: "a1b2c3d4e5f6...,f6e5d4c3b2a1..."
```

Specific repositories:
```
${repodigests;Maven Central;Local}
```

Check for changes:
```
repo.state=${repodigests}
```

Conditional on digest:
```
${if;${repodigests;Release};has-digest;no-digest}
```

## Use Cases

- Detecting repository changes
- Build cache invalidation
- Repository state tracking
- Dependency change detection
- Build reproducibility

## Notes

- Requires repository plugin support for digests
- Returns hex-encoded digest strings
- Comma-separated for multiple repos
- Silently skips unsupported repositories
- Errors on missing named repositories
- See also: `${repos}` for repository names

<hr />
TODO Needs review - AI Generated content

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
