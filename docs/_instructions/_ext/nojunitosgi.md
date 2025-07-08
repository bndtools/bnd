---
layout: default
class: Ant
title: -nojunitosgi  BOOLEAN
summary:  Indicate that this project does not have JUnit OSGi tests
---

The `-nojunitosgi` instruction indicates that the project does not contain JUnit OSGi tests. When this instruction is set to `true`, bnd will not attempt to run OSGi-based JUnit tests for the project. This is useful for projects that do not require OSGi test execution or do not have any test cases that need to be run in an OSGi environment.

By default, if test sources and test cases are present, bnd will attempt to run both standard and OSGi-based JUnit tests unless this instruction is specified.
