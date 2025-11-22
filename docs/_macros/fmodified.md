---
layout: default
class: Macro
title: fmodified ( ';' RESOURCE )+
summary: Get the latest modification timestamp from a list of files
---

## Summary

The `fmodified` macro finds the most recent modification time among a list of files and returns it as a long integer (milliseconds since epoch). This is useful for detecting when files have changed.

## Syntax

```
${fmodified;<file>[;<file>...]}
```

## Parameters

- `file` - One or more file paths (can be comma or semicolon-separated lists)

## Behavior

- Checks the last modified timestamp of each specified file
- Returns the latest (most recent) modification time found
- Returns the time in milliseconds since Unix epoch (January 1, 1970)
- Non-existent files are ignored
- Returns 0 if no files exist

## Examples

Get modification time of a single file:
```
${fmodified;build.gradle}
# Returns: 1700000000000 (example timestamp)
```

Find latest modification among multiple files:
```
${fmodified;src/Main.java;src/Util.java;src/Config.java}
# Returns timestamp of most recently modified file
```

Check if source files changed:
```
source.modified=${fmodified;${lsr;src;*.java}}
```

Compare modification times:
```
${if;${vcompare;${fmodified;input.txt};${fmodified;output.txt}};input-newer;output-newer}
```

Track build inputs:
```
Build-Input-Modified: ${fmodified;${buildpath}}
```

## Use Cases

- Detecting when source files have changed
- Implementing incremental build logic
- Tracking file dependencies
- Determining if regeneration is needed
- Caching and cache invalidation
- Build timestamp tracking

## Notes

- Returns milliseconds since epoch (not seconds)
- Non-existent files are silently ignored
- To convert to readable format, use `${long2date}` macro
- Returns 0 if all files are missing
- The timestamp represents the OS file system's last modified time
- Useful with `${if}` or `${vcompare}` for conditional logic



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
