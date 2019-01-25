---
title: Baselining
layout: default
requires: 2.2.0
---

Baselining compares a bundle with another bundle, the _baseline_, to find mistakes in the semantic versioning. For example, there is a binary incompatible change in the new bundle but the version has not been bumped. Baselining can be run from the command line (see `bnd help baseline`) or it can be run as part of a project build. 

APIs are compared for backward compatibility using the semantic versioning rules defined in this chapter. Baselining is aware of the `@ConsumerType` and `@ProviderType` rules. Proper versions are calculated and suggested.

## Setting Up a Project for Baselining

During the build, bnd will check the `-baseline` instruction at the end of the build when the JAR is ready. This instruction is a selector on the symbolic name of the building bundle. If it matches, the baselining is started with one exception: the bundle version must be more than 1.0.0. If the version is less no baselining is possible, the purpose is to allow the primordial baseline to be established without errors.

By default the baseline is a bundle from one of the repositories with the same symbolic name as the building bundle and the highest possible version. However, it is possible to specify the version or to baseline against a file, see [-baseline](../instructions/baseline.html).

It is recommended to enable baselining per project since not all project requires baselining. For example, baselining is overkill for a project that is always compiled with all its dependencies and thus has no external dependencies. The recommended practice is therefore to add the following instruction to a project that requires baselining (usually API bundles):

	-baseline: *

The default baseline is the bundle with the highest version in the _baseline repository_. The selector can specify a `version` or a `file` attribute when the default baseline is not applicable. 

* `version` – Baseline against the first bundle in the baseline repository that has the given version or higher.
* `file` – The file is the baseline.

The baseline bundle is looked up in the _baseline repository_. The baseline repository is by default the _release repository_ unless overridden by the [-baselinerepo](../instructions/baselinerepo.html) instruction. The release repository is set with the [-releaserepo](../instructions/releaserepo.html) instruction.

Only bundles that are not _staging_ are considered for the baseline, this means that it is possible to release the current bundle and compare against the previous version until the bundle is released and becomes master. 

By default the bundle and baseline are compared (_diffed_) and then analyzed for semantic version violations. Certain headers always change because they contain time or digest information. Most of these headers are already automatically ignored but the [-diffignore](../instructions/diffignore.html) instruction can add more ignorance. You can use the 
[-diffpackages](../instructions/diffpackages.html) instruction to specify the names of exported packages to be baseline compared. The default is to baseline compare all exported packages.

## Example baselining Project Instructions

      Bundle-Version: 1.0.2
      -baseline: *
      -baselinerepo: Released

In this example, the last version in the Released repository for the project's bsn is supposed to be the previous version. Make sure you do not always release staging versions to this repository since this will create false changes. During a development cycle, the baseline version must remain constant until the current development bundle is released, at which points it becomes the baseline of the next cycle.

Since an error is raised when the baselining detects a semantic version violation it is possible to release a snapshot in a build only when there is a correctly baselined bundle built.



