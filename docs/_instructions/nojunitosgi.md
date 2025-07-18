---
layout: default
title: -nojunitosgi  BOOLEAN
class: Ant
summary: |
   Indicate that this project does not have JUnit OSGi tests
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-nojunitosgi=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/nojunitosgi.md --><br /><br />

The `-nojunitosgi` instruction indicates that the project does not contain JUnit OSGi tests. When this instruction is set to `true`, bnd will not attempt to run OSGi-based JUnit tests for the project. This is useful for projects that do not require OSGi test execution or do not have any test cases that need to be run in an OSGi environment.

By default, if test sources and test cases are present, bnd will attempt to run both standard and OSGi-based JUnit tests unless this instruction is specified.


<hr />
TODO Needs review - AI Generated content
