---
layout: default
title: ide ';' ( 'javac.target' | 'javac.source' )
class: Project
summary: Read Java compiler settings from Eclipse IDE configuration
---

## Summary

The `ide` macro reads Java compiler source and target version settings from the Eclipse IDE's project preferences. This allows bnd builds to use the same Java version settings configured in the IDE.

## Syntax

```
${ide;<setting>[;<default>]}
```

## Parameters

- `setting` - The IDE setting to retrieve:
  - `javac.target` - The Java target bytecode version
  - `javac.source` - The Java source compatibility version
- `default` (optional) - Default value if the setting is not found

## Behavior

- Reads from `.settings/org.eclipse.jdt.core.prefs` in the project directory
- Lazy loads the preferences file on first use
- Maps setting names to Eclipse JDT compiler properties
- Returns the value or default if setting is not found
- Generates an error if the preferences file doesn't exist

## Examples

Get target bytecode version:
```
javac.target=${ide;javac.target}
# Returns: "1.8" (for Java 8) or "11" (for Java 11), etc.
```

Get source compatibility version:
```
javac.source=${ide;javac.source}
```

Use with default:
```
source.level=${ide;javac.source;1.8}
target.level=${ide;javac.target;1.8}
```

Configure bundle based on IDE settings:
```
Bundle-RequiredExecutionEnvironment: JavaSE-${ide;javac.target}
```

## Use Cases

- Synchronizing build settings with IDE configuration
- Ensuring consistent Java version across IDE and command-line builds
- Reading IDE preferences without duplicating configuration
- Portable projects that work across different development environments
- Maintaining consistency in multi-developer projects

## Notes

- Requires Eclipse JDT project structure (`.settings/org.eclipse.jdt.core.prefs`)
- The file is automatically created/managed by Eclipse IDE
- Only supports `javac.target` and `javac.source` settings
- Generates an error if the preferences file is missing
- Values are read from Eclipse's compiler settings
- Common values: "1.8", "11", "17", "21", etc.
- Other IDEs (IntelliJ, VS Code) may not create this file

<hr />
TODO Needs review - AI Generated content

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
