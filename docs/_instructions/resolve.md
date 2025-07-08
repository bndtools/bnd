---
layout: default
title: -resolve (manual|auto|beforelaunch|batch|cache)
class: Workspace
summary: |
   Defines when/how resolving is done to calculate the -runbundles
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-resolve: '-resolve manual`

- Values: `manual,auto,beforelaunch,batch,cache,never`

- Pattern: `(manual|auto|beforelaunch|batch)`

### Directives ###

- `manual`
  - Values: `manual`

  - Pattern: `\Qmanual\E`


- `auto`
  - Values: `auto`

  - Pattern: `\Qauto\E`


- `beforelaunch`
  - Values: `beforelaunch`

  - Pattern: `\Qbeforelaunch\E`


- `batch`
  - Values: `batch`

  - Pattern: `\Qbatch\E`


- `cache`
  - Values: `cache`

  - Pattern: `\Qcache\E`


- `never`
  - Values: `never`

  - Pattern: `\Qnever\E`

<!-- Manual content from: ext/resolve.md --><br /><br />

The bnd workspace can use a _resolver_ to calculate the content of the `-runbundles` instruction based on a set of _initial requirements_. The bndtools GUI can manually resolve the initial requirements but through the `-resolve` instruction it is possible to calculate the `-runbundles` when the file is saved or just before the `-runbundles` are used in the launch.

The values are:

* `manual` – It is up to the user to resolve the initial requirements
* `auto` – Whenever the initial requirements are saved, the resolver will be used to set new `-runbundles`
* `beforelaunch` – Calculate the `-runbundles` on demand. This ignores the value of the `-runbundles` and runs the resolver. The results of the resolver are cached. This cache works by creating a checksum over all the properties of the project.
* `batch` – When running in batch mode, the run bundles will be resolved. In all other modes this will only resolve when the `-runbundles` are empty.
* `cache` – Will use a cache file in the workspace cache. If that file is stale relative to the workspace or project or it does not exist, then the bnd(run) file will be resolved and the result is stored in the cache file.
* `never` – If anybody tries to resolve , the process will throw an `UnsupportedOperationException`. This is intended for manually curated runbundles or where a base file is resolved and other files include them an add some additional instructions. The Excepetion is meant as a clear warning to any developer accidentally resolving the `-runbundles`.


## Example

    -resolve beforelaunch

  
