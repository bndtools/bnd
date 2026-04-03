---
layout: default
class: Macro
title: -bumppolicy 
summary: The policy for the bump command
---

The `bump` command increases the versions in a bnd project. By default, it will use a _compatible policy_. A compatible to increment the minor part of the version and reset the micro part. The major part will remain the same. Using the `versionmask` syntax this looks like:

	=+0

The `-bumppolicy` macro can override this default. For example, the following instruction sets the  policy to make only major increments.

	+00
