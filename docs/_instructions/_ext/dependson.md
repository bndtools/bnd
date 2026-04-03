---
layout: default
class: Project
title: -dependson SELECTORS
summary: Add dependencies from the current project to other projects, before this project is built, any project this project depends on will be built first.
---

Projects referenced by [-buildpath](buildpath.html) are always built first but sometimes
you need to specify projects to be build first which are not referenced by `-buildpath`.
You can specify those additional projects using `-dependson`.

## Example

    -dependson: projA, projB
