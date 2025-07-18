---
layout: default
title: -releaserepo* NAME ( ',' NAME ) *
class: Project
summary: |
   Define the names of the repositories to use for a release
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-releaserepo=cnf`

- Values: `${repos}`

- Pattern: `.*`

<!-- Manual content from: ext/releaserepo.md --><br /><br />

You can specify zero or more repository names to use when releasing a project.

The `-releaserepo` instruction aggregates the `-releaserepo` property and all properties that start with `-releaserepo.`. This makes it possible to set release repository names in different places.

If the `-releaserepo` instruction is set to the empty value, then releasing a project does nothing. If the `-releaserepo` instruction is unset, then releasing a project will release it to the first writable repository.

Example:

```
-releaserepo: releases, snapshots
```

This instruction is useful for managing release targets in multi-repository environments.


<hr />
TODO Needs review - AI Generated content
