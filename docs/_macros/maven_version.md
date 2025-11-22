---
layout: default
class: Builder
title: maven_version ';' MAVEN-VERSION
summary: Cleanup a potential maven version to make it match an OSGi Version syntax.
---

## Summary

Convert a Maven-style version string to OSGi version format. **Note:** Use [version_cleanup](version_cleanup.html) instead, which can be used in more contexts.

## Syntax

    ${maven_version;<version>}

## Parameters

- **version**: Maven version string to convert to OSGi format

## Behavior

This macro is **deprecated** in favor of [version_cleanup](version_cleanup.html).

Converts Maven version formats (which may use hyphens) to OSGi version format (which uses dots):
- Replaces hyphens with dots in the qualifier
- Ensures the version follows OSGi's `major.minor.micro.qualifier` structure

For example: `1.2.3-SNAPSHOT` becomes `1.2.3.SNAPSHOT`

## Examples

```
# Convert Maven version
${maven_version;1.2.3-SNAPSHOT}
# Returns: 1.2.3.SNAPSHOT

# In Bundle-Version
Bundle-Version: ${maven_version;${project.version}}

# However, prefer version_cleanup:
Bundle-Version: ${version_cleanup;${project.version}}
```

## Deprecation Note

**This macro should not be used in new code.** Use [version_cleanup](version_cleanup.html) instead because:
- `version_cleanup` can be used in more contexts (not just Builder)
- `version_cleanup` handles more edge cases and version formats
- `version_cleanup` is the recommended approach going forward

## Related Macros

- [version_cleanup](version_cleanup.html) - **Preferred alternative** with broader applicability
- [versionmask](versionmask.html) - Transform versions using templates
- [version](version.html) - Alias for versionmask




---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
