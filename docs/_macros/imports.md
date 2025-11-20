---
layout: default
class: Analyzer
title: imports
summary: Get the list of packages imported by the current bundle
---

## Summary

The `imports` macro returns a comma-separated list of packages that will be imported by the current bundle. The list can be optionally filtered by providing filter arguments.

## Syntax

```
${imports}
${imports;<filter>...}
```

## Parameters

- `filter` (optional) - One or more filter patterns to select specific imported packages

## Behavior

- Returns all packages in the bundle's Import-Package header
- Packages are listed in a comma-separated format
- Optional filter arguments can narrow down the result
- Available during bundle analysis phase
- Includes both explicitly declared and automatically detected imports

## Examples

Get all imported packages:
```
imported.pkgs=${imports}
# Returns: "org.osgi.framework,javax.servlet,org.slf4j,..."
```

Document imported packages:
```
Bundle-Description: Depends on ${imports}
```

Count imported packages:
```
import.count=${size;${imports}}
```

Use in conditional logic:
```
${if;${imports};has-imports;no-imports}
```

Check for specific imports:
```
has.servlet=${if;${filter;${imports};javax\.servlet.*};yes;no}
```

## Use Cases

- Documenting bundle dependencies
- Validating import configurations
- Analyzing package dependencies
- Computing bundle coupling metrics
- Conditional logic based on what's imported
- Dependency verification in builds

## Notes

- Only includes packages in the Import-Package manifest header
- Includes both required and optional imports
- Packages listed here will be resolved at runtime by OSGi
- The list reflects analyzed/calculated imports, not just explicit declarations
- bnd automatically calculates imports by scanning bytecode
- See also: `${exports}` for exported packages
- See also: `${exporters}` to find which JARs provide packages




---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
