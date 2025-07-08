---
layout: default
class: Launcher
title: -runnoreferences  BOOLEAN
summary: Do not use the `reference:` URL scheme for installing a bundle in the installer.
---

# -runnoreferences

The `-runnoreferences` instruction controls whether the `reference:` URL scheme is used for installing bundles in the OSGi installer. On non-Windows systems, `reference:` URLs are used by default for efficiency. Setting `-runnoreferences: true` disables this behavior and uses regular file URLs instead. On Windows, this instruction is ignored because `reference:` URLs are never used due to file locking issues.

Example:

```
-runnoreferences: true
```

Use this instruction if you need to avoid `reference:` URLs for compatibility or deployment reasons.

