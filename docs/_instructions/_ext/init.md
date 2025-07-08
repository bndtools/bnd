---
layout: default
class: Project
title: -init ${MACRO} ( ',' ${MACRO}) * 
summary:  Executes the macros while initializing the project for building.
---

# -init

The `-init` instruction specifies one or more macros to execute when initializing the project for building. This ensures that certain setup steps are always performed before the build starts.

Example:

```
-init: ${my_macro}, ${my_macro2}
```

Macros are usually resolved and executed on demand, but those listed in `-init` are always executed during project initialization.
