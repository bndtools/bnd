---
layout: default
title: -dependson SELECTORS
class: Project
summary: |
   Add dependencies from the current project to other projects, before this project is built, any project this project depends on will be built first.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-dependson=org.acme.cm`

- Values: `${projects}`

- Pattern: `.*`

<!-- Manual content from: ext/dependson.md --><br /><br />

Projects referenced by [-buildpath](buildpath.html) are always built first but sometimes
you need to specify projects to be build first which are not referenced by `-buildpath`.
You can specify those additional projects using `-dependson`.

## Example

    -dependson: projA, projB
