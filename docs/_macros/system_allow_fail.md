---
layout: default
class: Macro
title: system_allow_fail ';' STRING ( ';' STRING )?
summary: Execute a system command, returning output and ignoring failures
---

## Summary

The `system-allow-fail` macro executes an operating system command and returns its output, but unlike `${system}`, it does not fail the build if the command returns a non-zero exit code. Failed commands generate a warning instead.

## Syntax

```
${system-allow-fail;<command>[;<input>]}
```

## Parameters

- `command` - The system command to execute
- `input` (optional) - Text to send to the command's standard input

## Behavior

- Executes the command in the project base directory
- On Windows, automatically wraps command with `cmd /c`
- Captures and returns standard output (trimmed)
- If input is provided, writes it to stdin
- Returns empty string if command fails (no build error)
- Generates warning (not error) on non-zero exit codes
- Build continues even on command failure

## Examples

Try to get Git info (may not be in repo):
```
Git-Branch: ${system-allow-fail;git branch --show-current}
```

Optional version check:
```
tool.version=${system-allow-fail;tool --version}
```

Check for optional tool:
```
${if;${system-allow-fail;which docker};docker-available;docker-not-found}
```

Get info with fallback:
```
host=${def;${system-allow-fail;hostname};unknown-host}
```

## Use Cases

- Running commands that may not be available
- Optional build information gathering
- Checking for tool availability
- Platform-specific commands that may fail
- Best-effort information collection
- Graceful degradation in builds

## Notes

- **Does not fail the build** on command errors
- Returns empty string on failure
- Generates warning (visible in logs) on failure
- Same execution model as `${system}`
- Useful when command availability is uncertain
- See also: `${system}` for failing variant
- May be disabled in restricted execution modes
	
<hr />
TODO Needs review - AI Generated content

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
