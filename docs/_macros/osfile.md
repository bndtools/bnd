---
layout: default
class: Macro
title: osfile ';' DIR ';' NAME
summary: Create an OS-specific absolute file path
---

## Summary

The `osfile` macro constructs an absolute file path by combining a base directory and a relative path, using the operating system's native path separator (e.g., backslash on Windows, forward slash on Unix).

## Syntax

```
${osfile;<base>;<path>}
```

## Parameters

- `base` - The base directory path
- `path` - The relative path to append to the base

## Behavior

- Combines the base directory and relative path
- Uses the OS-native path separator
- Returns an absolute path
- Creates proper path syntax for the current platform

## Examples

Create OS-specific path on Unix:
```
${osfile;/home/user;project/src/Main.java}
# Returns: "/home/user/project/src/Main.java"
```

Create OS-specific path on Windows:
```
${osfile;C:/Users/user;project\\src\\Main.java}
# Returns: "C:\Users\user\project\src\Main.java"
```

Build path from project base:
```
config.file=${osfile;${basedir};config/app.properties}
```

Create platform-specific classpath entry:
```
jar.path=${osfile;${basedir};lib/dependency.jar}
```

## Use Cases

- Creating platform-independent path references
- Building absolute paths for external tools
- Generating OS-specific file references
- Creating portable build scripts
- Path construction in cross-platform projects
- Interfacing with OS-specific tools

## Notes

- The resulting path uses the OS-native separator
- Windows paths use backslash (\), Unix uses forward slash (/)
- Returns an absolute path, not a relative one
- Handles path separator conversion automatically
- See also: `${path}` for Unix-style paths
- See also: `${pathseparator}` for the OS path separator
- See also: `${basedir}` for getting the project base directory


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
