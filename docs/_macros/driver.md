---
layout: default
class: Workspace
title: driver ( ';' NAME )?
summary: Get or check the build environment driver (gradle, eclipse, intellij, etc.)
---

## Summary

The `driver` macro identifies the build environment (driver) that is running bnd, such as Gradle, Ant, Eclipse, or IntelliJ. It can be used to return the driver name or check if the current driver matches specific names.

The driver should be set when bnd is started by the build tool and can be overridden with the `-bnd-driver` property. This is useful for [avoiding target-dir conflicts between different build tools](/chapters/150-build.html#avoiding-target-dir-conflicts-between-different-build-tools).

## Syntax

```
${driver[;<name>...]}
```

## Parameters

- `name` (optional) - One or more driver names to check against. If provided, returns the driver name only if it matches one of the arguments; otherwise returns empty string.

## Behavior

- Without arguments: Returns the driver name (e.g., "gradle", "eclipse")
- With arguments: Returns driver name if it matches any argument (case-insensitive), empty string otherwise
- Driver can be set via `Workspace.setDriver()` or `-bnd-driver` property
- Returns "unset" if no driver is configured

## Examples

Get current driver:
```
${driver}
# Returns: "gradle" (if running under Gradle)
```

Check if running under Gradle:
```
${if;${driver;gradle};gradle-specific-config;}
```

Check multiple drivers:
```
${driver;gradle;maven}
# Returns: "gradle" if gradle, or "maven" if maven, otherwise ""
```

Conditional configuration:
```
-target-dir: ${if;${driver;gradle};gradle-build;${if;${driver;eclipse};eclipse-build;build}}
```

Set via property:
```
-bnd-driver: custom-driver
```

## Use Cases

- Tool-specific configuration
- Avoiding build tool conflicts
- Different output directories per tool
- Build environment detection
- Tool-specific optimizations
- IDE vs command-line differences

## Notes

- Driver names are case-insensitive for matching
- Can be overridden with `-bnd-driver` property
- Returns empty string (falsy) if no match
- Default is "unset" if not configured
- Commonly used values: gradle, maven, ant, eclipse, intellij, cli
	



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
