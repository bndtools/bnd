---
layout: default
class: Analyzer
title: frange ';' VERSION ( ';' BOOLEAN )?
summary: Generate an OSGi filter expression for a version range
---

## Summary

The `frange` macro generates an OSGi filter expression representing a version range. By default, it creates a consumer-compatible range (major version increment). Optionally, you can request a provider-compatible range (minor version increment).

## Syntax

```
${frange;<version>[;provider]}
${frange;<version-range>}
```

## Parameters

- `version` - A version string (e.g., "1.2.3") or a version range (e.g., "[1.2.3,2.0.0)")
- `provider` (optional) - Boolean (true/false) to use provider compatibility (default: false for consumer compatibility)

## Behavior

- **Consumer compatibility (default)**: Creates range from version to next major version
  - `${frange;1.2.3}` → `(&(version>=1.2.3)(!(version>=2.0.0)))`
- **Provider compatibility**: Creates range from version to next minor version
  - `${frange;1.2.3;true}` → `(&(version>=1.2.3)(!(version>=1.3.0)))`
- **Version range**: If a version range is provided, converts it directly to filter syntax
  - `${frange;[1.2.3,2.3.4)}` → `(&(version>=1.2.3)(!(version>=2.3.4)))`

## Examples

Consumer compatibility range:
```
Import-Package: org.example.*;version="${frange;1.2.3}"
# Generates: (&(version>=1.2.3)(!(version>=2.0.0)))
```

Provider compatibility range:
```
Require-Capability: osgi.service;filter:="${frange;1.0.0;true}"
# Generates: (&(version>=1.0.0)(!(version>=1.1.0)))
```

Use with version property:
```
bundle.range=${frange;${bundle.version}}
```

Dependency filter:
```
Require-Bundle: com.example.api;bundle-version="${frange;2.5.0}"
```

Version range input:
```
${frange;[1.0.0,3.0.0)}
# Converts range to filter syntax
```

## Use Cases

- Generating version filters for Import-Package headers
- Creating compatibility ranges for bundle dependencies
- Specifying service version requirements
- Expressing OSGi semantic versioning ranges
- Building capability filters with version constraints
- Defining compatible version ranges in manifests

## Notes

- Consumer compatibility follows OSGi semantic versioning (breaking changes at major version)
- Provider compatibility is more restrictive (breaking changes at minor version)
- The generated filter uses OSGi filter syntax with `version` attribute
- Qualifiers are removed from version bounds
- Can accept either a version or a version range as input
- See also: `${range}` for generating version range notation (not filter syntax)



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
