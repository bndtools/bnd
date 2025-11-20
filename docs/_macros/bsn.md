---
layout: default
class: Analyzer
title: bsn
summary: Get the Bundle Symbolic Name (BSN) of the current bundle being built
---

## Summary

The `bsn` macro returns the Bundle Symbolic Name (BSN) of the bundle currently being analyzed or built. This is particularly useful in sub-bundle scenarios where the BSN may differ from the project's main BSN.

## Syntax

```
${bsn}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns the Bundle-SymbolicName of the bundle being generated
- In simple projects, this is typically the project's BSN
- In projects with sub-bundles, returns the specific sub-bundle's BSN
- Available during the JAR generation phase

## Examples

Use the BSN in a manifest header:
```
Bundle-Description: Bundle for ${bsn}
```

Create BSN-based paths:
```
output.file=${bsn}.jar
```

Reference in bundle documentation:
```
Bundle-DocURL: https://example.com/docs/${bsn}
```

## Use Cases

- Creating bundle-specific configuration
- Generating bundle-aware documentation URLs
- Building output file names based on BSN
- Conditional logic based on which bundle is being built
- Sub-bundle configurations that need to reference their own BSN

## Notes

- The BSN is the unique identifier for an OSGi bundle
- This differs from `${project.name}` which is the project name
- In multi-bundle projects (with `.bnd` files), each sub-bundle has its own BSN
- The BSN follows OSGi naming conventions (typically reverse domain notation)


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
