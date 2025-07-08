---
layout: default
title: -donotcopy
class: Project
summary: |
   Set the default filters for file resources that should not be copied.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-donotcopy=(CVS|\.svn)`

- Pattern: `.*`

<!-- Manual content from: ext/donotcopy.md --><br /><br />

When `-includeresource` copies files from another JAR or the file system it will look at the `-donotcopy` 
instruction. This instruction is a _single_ regular expression. Any resource that is copied is matched
against this list. If there is a match, then the file is ignored.

For example (and also the defaults)

    -donotcopy: CVS|\\.svn|\\.git|\\.DS_Store|\\.gitignore
