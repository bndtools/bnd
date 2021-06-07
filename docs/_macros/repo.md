---
layout: default
class: Project
title: repo ';' BSN ( ';' VERSION ( ';' STRATEGY )? )?
summary: Provides the file paths to artifact in the repositories
---

Returns the absolute file system paths to the specified artifacts in the repositories.

BSN is be a comma-separated list of bundle symbolic names. If the artifact
is not a bundle, then the synthetic bundle symbolic names of groupId:artifactId
can be used. Normally only a single bundle symbolic name is used since the remainder of the options apply to all the bundle symbolic names.

VERSION is a version range for the artifact. Special values supported are:
* `project` - This return the built artifact from a project in the Bnd workspace.
* `snapshot` - Synonym for `project`.
* `latest` - The highest version available in a project in the Bnd workspace or the repositories. The built artifact from a project in the Bnd workspace is always used if it exists under the assumption the Bnd workspace is always building the latest version of the artifact.
If the version range is not specified, the version range `[0,∞)` is used.

STRATEGY is the selection strategy to be used when multiple artifacts with the bundle symbolic name exist within the version range. The strategies supported are:
* `HIGHEST` - The highest version for the artifact which is included by the version range. This is the default strategy and is the strategy always used by the special version range `latest`.
* `LOWEST` - The lowest version for the artifact which is included by the version range.
* `EXACT` - When this strategy is used, the version range must be a single version which is the version which is searched for. If multiple repositories contain the the exact version of the artifact, the artifact from the first repository is used.
