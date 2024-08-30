---
layout: default
class: Macro
title: system-in-path-allow-fail ';' PATH ';' CMD ( ';' INPUT )?
summary: Execute a system command in the given path but ignore any failures
---

Executes a System command in the given path. The path can be anywhere, even outside the current Project. 
This can be used to execute command line tools. If an INPUT is given, it will be given to the process via the Standard Input. 

If the Process exits with anything other  than `0`, the result will be be marked as a warning. 

Usage Example:
```
# Extracts the current git SHA in the given path
Git-SHA: ${system-in-path-allow-fail; ~/git/someproject; git rev-list -1 --no-abbrev-commit HEAD}

# Extracts the current git SHA in the given path and enters the password as input
Git-SHA: ${system-in-path-allow-fail; ~/git/someproject; git rev-list -1 --no-abbrev-commit HEAD;mypassword}
```