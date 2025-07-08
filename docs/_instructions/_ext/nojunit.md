---
layout: default
class: Ant
title: -nojunit  BOOLEAN
summary:  Indicate that this project does not have JUnit tests
---

The `-nojunit` instruction indicates that the project does not contain JUnit tests. When this instruction is set to `true`, bnd will not attempt to run JUnit tests for the project, even if test sources are present.

This is useful for projects that do not require unit testing or do not have any test cases that need to be executed as part of the build process.
