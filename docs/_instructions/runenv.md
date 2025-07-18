---
layout: default
title: -runenv PROPERTIES
class: Project
summary: |
   Specify a JDB port on invocation when launched outside a debugger so the debugger can attach later.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runenv: org.osgi.service.http.port=9999, org.osgi.framework.bootdelegation="sun.*,com.sun.*,"`

- Pattern: `.*`

<!-- Manual content from: ext/runenv.md --><br /><br />

# -runenv

The `-runenv` instruction specifies system properties to set when launching the application. This is useful for configuring the runtime environment, such as setting ports, feature flags, or other JVM properties.

Example:

```
-runenv: my.property=value, another.property=1234
```

These properties will be set in the local JVM when the workspace is started.


TODO Needs review - AI Generated content
