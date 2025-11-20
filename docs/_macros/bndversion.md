---
layout: default
class: Macro
title: bndversion
summary: Returns the current running bnd version as full major.minor.micro
---

## Summary

Returns the version number of the bnd tool currently being used to process the build.

## Syntax

    ${bndversion}

## Parameters

None. This macro takes no parameters.

## Behavior

Returns the current bnd version as a string in the format `major.minor.micro` (without the qualifier/build timestamp).

For example: `7.0.0` or `6.4.0` or `5.3.0`

## Examples

```
# Display bnd version in bundle manifest
Bundle-BuildTool: bnd-${bndversion}

# Conditional logic based on bnd version
-include ${if;${vcompare;${bndversion};7.0.0};modern.bnd;legacy.bnd}

# Document which bnd version was used
Bundle-Comment: Built with bnd ${bndversion}

# Version-specific features
-conditionalpackage: ${if;${vcompare;${bndversion};6.4.0};com.new.*;com.legacy.*}
```

## Use Cases

1. **Build Documentation**: Record which version of bnd was used to build a bundle
2. **Conditional Configuration**: Apply different settings based on bnd version
3. **Feature Detection**: Enable features only available in newer bnd versions
4. **Troubleshooting**: Help identify build environment for debugging
5. **Version Requirements**: Verify minimum bnd version requirements

## Notes

- The version returned does not include the qualifier (e.g., build timestamp or snapshot indicator)
- This is the runtime version of bnd, not a property that can be set
- Useful for ensuring reproducible builds when combined with version checks
- Can be combined with [vcompare](vcompare.html) for version comparisons

## Related Macros

- [vcompare](vcompare.html) - Compare version strings
- [version](version.html) - Format version numbers with masks 


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
