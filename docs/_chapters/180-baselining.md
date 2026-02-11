---
title: Baselining
layout: default
requires: 2.2.0
---

Baselining compares a bundle with another bundle, the _baseline_, to find mistakes in the [semantic versioning](https://docs.osgi.org/whitepaper/semantic-versioning/). For example, there is a binary incompatible change in the new bundle but the version has not been bumped. Baselining can be run from the command line (see `bnd help baseline`) or it can be run as part of a project build. 

APIs are compared for backward compatibility using the semantic versioning rules defined in this chapter. Baselining is aware of the `@ConsumerType` and `@ProviderType` rules. Proper versions are calculated and suggested.

## Setting Up a Project for Baselining

During the build, bnd will check the [`-baseline` instruction](../instructions/baseline.html) at the end of the build when the JAR is ready. This instruction is a selector on the symbolic name of the building bundle. If it matches, the baselining is started with one exception: by default, the bundle/package version must be 1.0.0 or above. If the version is less (i.e. major version being `0`) no baselining errors are reported, the purpose is to allow the [primordial baseline to be established without errors](https://semver.org/#spec-item-4).

To enable baselining for versions in the range `[0.1.0, 1.0.0)`, use the [`-baselineincludezeromajor` instruction](../instructions/baselineincludezeromajor.html):

	-baselineincludezeromajor: true

This will enable baseline error reporting for packages with major version `0` (except for `0.0.x` versions which are still excluded).

By default the baseline is a bundle from one of the repositories with the same symbolic name as the building bundle and the highest possible version. However, it is possible to specify the version or to baseline against a file.

It is recommended to enable baselining per project since not all project requires baselining. For example, baselining is overkill for a project that is always compiled with all its dependencies and thus has no external dependencies. The recommended practice is therefore to add the following instruction to a project that requires baselining (usually API bundles):

	-baseline: *

The default baseline is the bundle with the highest version in the _baseline repository_. The selector can specify a `version` or a `file` attribute when the default baseline is not applicable. 

* `version` – Baseline against the first bundle in the baseline repository that has the given version or higher.
* `file` – The file is the baseline.

The baseline bundle is looked up in the _baseline repository_. The baseline repository is by default the _release repository_ unless overridden by the [-baselinerepo](../instructions/baselinerepo.html) instruction. The release repository is set with the [-releaserepo](../instructions/releaserepo.html) instruction.

Only bundles that are not _staging_ are considered for the baseline, this means that it is possible to release the current bundle and compare against the previous version until the bundle is released and becomes master. 

By default the bundle and baseline are compared (_diffed_) and then analyzed for semantic version violations. Certain headers always change because they contain time or digest information. Most of these headers are already automatically ignored but the [-diffignore](../instructions/diffignore.html) instruction can add more ignorance. You can use the 
[-diffpackages](../instructions/diffpackages.html) instruction to specify the names of exported packages to be baseline compared. The default is to baseline compare all exported packages.

### Maven Support

There is a dedicated [Maven plugin for baselining](../../maven-plugins/bnd-baseline-maven-plugin/README.md).

## Example baselining Project Instructions

      Bundle-Version: 1.0.2
      -baseline: *
      -baselinerepo: Released

In this example, the last version in the Released repository for the project's bsn is supposed to be the previous version. Make sure you do not always release staging versions to this repository since this will create false changes. During a development cycle, the baseline version must remain constant until the current development bundle is released, at which points it becomes the baseline of the next cycle.

Since an error is raised when the baselining detects a semantic version violation it is possible to release a snapshot in a build only when there is a correctly baselined bundle built.

### `@BaselineIgnore` - Fine grained control

Occasionally, scenarios arise where a language construct or change is not accounted for in bnd (bnd developers are humans too), or where a developer wants to overrule the strict opinion of the baseline logic for whatever reason (not recommended but it does happen) for instance in the grey areas of binary compatibility.

Instead of forcing developers to make a choice between disabling baseline (in order to avoid build warnings or failures) and keeping it enabled it's important to know that there is fine grained control available using bnd's `@aQute.bnd.annotation.baseline.BaselineIgnore` annotation.

The value of the `@BaselineIgnore` annotation is a valid OSGi version string.

e.g.

```java
@BaselineIgnore("2.4.12")
public Foo getFoo();
```

When the `@BaselineIgnore` annotation is applied to a *baselined* element, the *baseliner* will ignore the annotated element when baselining against a baseline package whose version is less than the specified version. This means the annotated element will not produce a baselining mismatch. The correct baseline information about the element will be in the baseline report, but the element will not cause baselining to fail. When baselining against a baseline package whose version is greater than or equal to the specified version, this annotation is ignored and the annotated element will be included in the baselining.

The annotation should be used in a scope that is as narrow as possible by applying it to the most specific member causing the baseline _issue_.
