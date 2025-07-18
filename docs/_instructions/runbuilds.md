---
layout: default
title: -runbuilds BOOLEAN
class: Project
summary: |
   Defines if this should add the bundles build by this project to the -runbundles. For a bndrun file this is default false, for a bnd file this is default true.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runbuilds=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/runbuilds.md --><br /><br />

The `-runbuilds` instruction controls whether the bundles built by the current project should be automatically added to the `-runbundles` list when running or testing the project. 

- In a `.bndrun` file, the default is `false`, so built bundles are not added unless you explicitly set `-runbuilds: true`.
- In a `.bnd` file, the default is `true`, so built bundles are included unless you set `-runbuilds: false`.

This instruction is useful for controlling which bundles are available at runtime, especially when you want to test or launch only a subset of the bundles produced by your project.


<hr />
TODO Needs review - AI Generated content
