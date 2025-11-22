---
layout: default
title: exporters ';' PACKAGE
class: Analyzer
summary: List JARs on the classpath that export a given package
---

## Summary

The `exporters` macro returns a comma-separated list of JAR names from the classpath that contain (export) a specified package. This is useful for discovering which dependencies provide a particular package.

## Syntax

```
${exporters;<package-name>}
```

## Parameters

- `package-name` - The fully qualified package name (e.g., "com.example.api")

## Behavior

- Searches all JARs on the build classpath
- Checks each JAR for directories matching the specified package
- Returns a comma-separated list of JAR names that contain the package
- Package name uses dot notation (e.g., "com.example") which is converted internally to path format
- Returns empty string if no JARs export the package

## Examples

Find which JARs provide a specific package:
```
slf4j.jars=${exporters;org.slf4j}
# Returns: "slf4j-api-1.7.30"
```

Check if a package is available:
```
${if;${exporters;javax.servlet};servlet-available;servlet-missing}
```

List providers of multiple packages:
```
json.providers=${exporters;org.json}
jackson.providers=${exporters;com.fasterxml.jackson.core}
```

Document dependencies:
```
# Package javax.xml.bind provided by: ${exporters;javax.xml.bind}
```

## Use Cases

- Discovering which dependencies provide specific packages
- Debugging classpath and dependency issues
- Documenting package sources
- Conditional configuration based on available packages
- Validating that required packages are on the classpath
- Analyzing dependency conflicts

## Notes

- Only searches JARs on the build classpath (not runtime classpath)
- Package name should use dot notation (e.g., "org.osgi.framework")
- Returns JAR filenames, not full paths
- Multiple JARs may export the same package (split packages)
- Empty return value indicates the package is not found on the classpath



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
