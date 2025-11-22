---
layout: default
class: Analyzer
title: exports
summary: Get the list of packages exported by the current bundle
---

## Summary

The `exports` macro returns a comma-separated list of packages that will be exported by the current bundle. The list can be optionally filtered by providing filter arguments.

## Syntax

```
${exports}
${exports;<filter>...}
```

## Parameters

- `filter` (optional) - One or more filter patterns to select specific exported packages

## Behavior

- Returns all packages in the bundle's Export-Package header
- Packages are listed in a comma-separated format
- Optional filter arguments can narrow down the result
- Available during bundle analysis phase

## Examples

Get all exported packages:
```
exported.pkgs=${exports}
# Returns: "com.example.api,com.example.impl,com.example.util"
```

Document exported packages:
```
Bundle-Description: Exports ${exports}
```

Count exported packages:
```
export.count=${size;${exports}}
```

Use in conditional logic:
```
${if;${exports};has-exports;no-exports}
```

Filter exported packages:
```
# Get only API packages (if filter parameters are supported)
api.exports=${exports;.*\.api}
```

## Use Cases

- Documenting which packages are exposed by a bundle
- Validating export configurations
- Generating bundle documentation
- Computing bundle metrics (number of exported packages)
- Conditional logic based on what's exported
- Quality checks for proper API exposure

## Notes

- Only includes packages in the Export-Package manifest header
- Packages listed here are publicly accessible to other bundles
- Private packages are not included
- The list reflects the analyzed/calculated exports, not necessarily what was explicitly declared
- See also: `${imports}` for imported packages



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
