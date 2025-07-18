---
layout: default
class: Project
title: -runenv PROPERTIES
summary: Specify a JDB port on invocation when launched outside a debugger so the debugger can attach later. 
---

# -runenv

The `-runenv` instruction specifies system properties to set when launching the application. This is useful for configuring the runtime environment, such as setting ports, feature flags, or other JVM properties.

Example:

```
-runenv: my.property=value, another.property=1234
```

These properties will be set in the local JVM when the workspace is started.


---
TODO Needs review - AI Generated content