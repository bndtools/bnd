---
layout: default
class: Macro
title: system-allow-fail ';' CMD ( ';' INPUT )?
summary: Execute a system command but ignore any failures
---

Executes a System command in the current project directory. 
This can be used to execute command line tools. If an INPUT is given, it will be given to the process via the Standard Input. 

If the Process exits with anything other  than `0`, the result will be be marked as a warning. 

Usage Example:
```
# Extracts the current git SHA for the Project
Git-SHA: ${system-allow-fail;git rev-list -1 --no-abbrev-commit HEAD}

# Extracts the current git SHA for the Project and enters the password as input
Git-SHA: ${system-allow-fail;git rev-list -1 --no-abbrev-commit HEAD;mypassword}
```