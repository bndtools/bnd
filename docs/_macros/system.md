---
layout: default
class: Macro
title: system ';' STRING ( ';' STRING )?
summary: Execute a system command and return its output
---

## Summary

The `system` macro executes an operating system command and returns its standard output. The command runs in the project's base directory. The build fails if the command returns a non-zero exit code.

## Syntax

```
${system;<command>[;<input>]}
```

## Parameters

- `command` - The system command to execute
- `input` (optional) - Text to send to the command's standard input

## Behavior

- Executes the command in the project base directory
- On Windows, automatically wraps command with `cmd /c`
- Captures and returns standard output (trimmed)
- If input is provided, writes it to the command's stdin
- Returns empty string and generates error if command fails
- Build fails on non-zero exit codes

## Examples

Get Git commit hash:
```
Git-Commit: ${system;git rev-parse HEAD}
```

Get current date:
```
Build-Date: ${system;date}
```

Execute command with input:
```
${system;wc -l;line1\nline2\nline3}
```

Get Maven version:
```
mvn.version=${system;mvn --version}
```

List files:
```
files=${system;ls -la}
```

## Use Cases

- Embedding command output in manifests
- Getting version control information
- Capturing build environment details
- Running custom build scripts
- Platform-specific build information
- Integration with external tools

## Notes

- **Security Warning**: Be cautious with user input in commands
- Commands run in the project base directory
- Windows commands are automatically wrapped with `cmd /c`
- Standard output is captured and trimmed
- Standard error is not captured
- Build fails on command failure
- See also: `${system-allow-fail}` for non-failing variant
- May be disabled in restricted execution modes
	

<hr />
TODO Needs review - AI Generated content

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
