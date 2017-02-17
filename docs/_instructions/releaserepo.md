---
layout: default
class: Project
title: -releaserepo* NAME ( ',' NAME ) *
summary: Define the names of the repositories to use for a release
---

You can specify zero or more repository names to use when releasing a project.

The `-releaserepo` instruction aggregates the `-releaserepo` property and all properties that start with `-releaserepo.`. This makes it possible to set release repository names in different places.

If the `-releaserepo` instruction is set to the empty value, then releasing a project does nothing. If the `-releaserepo` instruction is unset, then releasing a project will release it to the first writable repository.
