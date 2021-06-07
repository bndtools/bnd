---
layout: default
class: Workspace
title: -noparallel CATEGORY;task=TASKS
summary: Prevent Gradle tasks in the same category from executing in parallel.
---

Gradle supports `--parallel` to run build tasks in parallel when possible. This can be a great speed improvement for a build. But sometimes, certain tasks cannot be run in parallel due to certain resource contention.

The `-noparallel` Bnd instruction can be used to state that any tasks assigned to a category must not be run in parallel with any other task assigned to the same category. The category names are open ended. Any task names specified must be the names of actual Gradle tasks in the project. Multiple categories and multiple task names per category can be specified.

For example:

	-noparallel: launchpad;task="test", port80;task="testOSGi"
