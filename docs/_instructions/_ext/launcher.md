---
layout: default
class: Project
title: -launcher
summary: Options for the runtime launcher
---

This instruction has as purpose to collect options and special settings for the launcher. The
following options are architected:

* `manage`  – Indicates to the launcher how to treat unrecognized bundles. When the launcher starts 
  it gets a list of run bundles, also called its _scope_. However, previous runs could've installed
  other bundles that do not occur in the scope. By default the launcher should manage _all_
  bundles. However, sometimes these bundles were installed by an agent and the launcher should
  not touch them. Therefore the values for the 'manage' part are:
  * `all` – This is the default and the launcher assumes it owns all the bundles
  * `narrow` – The launcher will only touch the bundles that are part of its scope
  * `none` – The launcher should defer from managing any bundles. 
  
## Example

    -launcher manage = all  


## Background

This instruction was primarily designed to handle start levels. Originally the launcher was
_narrowly_ managing only the bundles that were in its scope. However, this was inadvertently
changed and not discovered for several reasons. The option to narrowly manage was therefore
introduced with the default being the latest behavior.

