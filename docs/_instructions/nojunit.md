---
layout: default
title: -nojunit  BOOLEAN
class: Ant
summary: |
   Indicate that this project does not have JUnit tests
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-nojunit=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/nojunit.md --><br /><br />

The `-nojunit` instruction indicates that the project does not contain JUnit tests. When this instruction is set to `true`, bnd will not attempt to run JUnit tests for the project, even if test sources are present.

This is useful for projects that do not require unit testing or do not have any test cases that need to be executed as part of the build process.


TODO Needs review - AI Generated content
