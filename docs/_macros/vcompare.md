---
layout: default
class: Macro
title: vcompare VERSION VERSION
summary: Compare two version strings 
---

## Summary

Compare two OSGi version strings using proper version semantics, returning -1, 0, or 1.

## Syntax

    ${vcompare;<version1>;<version2>}

## Parameters

- **version1**: First version to compare (OSGi version format)
- **version2**: Second version to compare (OSGi version format)

## Behavior

Compares two OSGi version strings using semantic version comparison and returns:
- `0` - The versions are equal
- `1` - The first version is greater than the second (newer)
- `-1` - The first version is less than the second (older)

OSGi versions follow the format: `major.minor.micro.qualifier`
- Numeric parts (major, minor, micro) are compared numerically
- Qualifier is compared lexicographically

## Examples

```
# Equal versions
${vcompare;1.0.0;1.0.0}
# Returns: 0

# First version greater
${vcompare;2.0.0;1.5.0}
# Returns: 1

# First version less
${vcompare;1.0.0;2.0.0}
# Returns: -1

# Micro version comparison
${vcompare;1.2.3;1.2.2}
# Returns: 1

# With qualifiers
${vcompare;1.0.0.SNAPSHOT;1.0.0.RELEASE}
# Returns: 1 (SNAPSHOT > RELEASE lexicographically)

# Different lengths
${vcompare;1.0;1.0.0}
# Returns: 0 (1.0 equals 1.0.0.0)

# Use in conditional logic
-include ${if;${vcompare;${bndversion};7.0.0};modern.bnd;legacy.bnd}

# Check minimum version requirement
-include ${if;${vcompare;${Bundle-Version};2.0.0};compatible.bnd;incompatible.bnd}

# Version range check
is-current = ${if;${vcompare;${Bundle-Version};3.0.0};true;false}

# Compare with project version
${vcompare;${version;===;${Bundle-Version}};${minimum-version}}
```

## Use Cases

1. **Version Requirements**: Check if a version meets minimum requirements
2. **Conditional Configuration**: Apply different settings based on version
3. **Build Compatibility**: Ensure compatibility with specific versions
4. **Version Validation**: Verify version constraints are met
5. **Feature Gating**: Enable features only in certain version ranges

## Notes

- Uses **OSGi version semantics**, not lexicographic comparison
- For string comparison, use [compare](compare.html)
- For numeric comparison, use [ncompare](ncompare.html)
- Properly handles major.minor.micro.qualifier format
- Missing version parts default to 0 (or empty qualifier)
- Numeric parts compared numerically (2.0 > 1.10)
- Qualifier compared alphabetically
- Returns only -1, 0, or 1

## Related Macros

- [compare](compare.html) - Compare strings lexicographically
- [ncompare](ncompare.html) - Compare numbers numerically
- [version](version.html) / [versionmask](versionmask.html) - Format version numbers
- [if](if.html) - Conditional macro that can use comparison results
- [bndversion](bndversion.html) - Get current bnd version for comparison



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
