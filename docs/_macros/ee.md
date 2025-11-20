---
layout: default
class: Analyzer
title: ee
summary: Get the highest Java Execution Environment (EE) required by the bundle
---

## Summary

The `ee` macro returns the name of the highest Java Execution Environment (EE) required by the classes in the current bundle being analyzed. This is determined by analyzing the bytecode version of all classes in the JAR.

## Syntax

```
${ee}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Analyzes all class files in the bundle
- Determines the highest Java class file version used
- Returns the corresponding EE name (e.g., "JavaSE-1.8", "JavaSE-11", "JavaSE-17")
- Available during bundle analysis and JAR generation

## Examples

Set the Bundle-RequiredExecutionEnvironment based on analyzed classes:
```
Bundle-RequiredExecutionEnvironment: ${ee}
```

Use in conditional logic:
```
# Check if Java 11+ is required
java11plus=${if;${vcompare;${ee};JavaSE-11};true;false}
```

Document the requirement:
```
Bundle-Description: Requires ${ee} or higher
```

## Use Cases

- Automatically determining minimum Java version requirements
- Validating that bundle dependencies match execution environment
- Generating accurate OSGi manifest headers
- Documenting Java version requirements
- Ensuring compatibility across different Java versions

## Notes

- The EE is determined by bytecode analysis, not source code
- Returns the *highest* (most recent) EE found among all classes
- The EE name follows OSGi execution environment naming conventions
- Common values include: JavaSE-1.8, JavaSE-11, JavaSE-17, JavaSE-21
- This is typically used to set the `Bundle-RequiredExecutionEnvironment` header




---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
