---
layout: bnd
title: Architecture Constraints
parent: Architecture (arc42)
grand_parent: Developing bnd and Bndtools
nav_order: 2
---

# 2. Architecture Constraints

## 2.1 Technical constraints

| Constraint | Consequence |
|---|---|
| OSGi metadata and resolution semantics | Packages, capabilities, requirements, bundle identity, and versions are first-class architecture concepts |
| Java 17 baseline on `master` | Core and current plugins may use Java 17; the `classic` branch carries older Java support |
| Eclipse plug-in model | Bndtools components are OSGi bundles using Eclipse extension points, services, resources, jobs, editors, launch APIs, and p2 |
| Maven and Gradle lifecycle models | Each adapter must translate native lifecycle inputs into bnd models without creating divergent semantics |
| Gradle 8.0 minimum for current plugins | Plugin implementation can use current Gradle APIs, but workspace mode cannot support configuration cache because bnd model objects are not serializable |
| Maven plugins are built with Maven | Workspace artifacts must be built and installed before the Maven reactor |
| Public artifacts are consumed independently | Core libraries, CLI, build plugins, Eclipse features, launcher, tester, and repository modules require controlled versioning |
| Multi-platform use | Paths, process launch, file watching, and repository access must work across Windows, Linux, and macOS |

## 2.2 Organizational constraints

- Apache License 2.0 or Eclipse Public License 2.0 licensing.
- Contributions arrive through pull requests and must preserve compatibility.
- `master` produces development snapshots; releases are prepared through the
  project release flow.
- Maven Central, Gradle Plugin Portal, JFrog repositories, and Eclipse p2 serve
  different artifact formats and audiences.
- OSGi semantic versioning and bnd's own baseline checks govern public package
  evolution.
- Documentation is split between the bnd site in this repository and the
  Bndtools website repository.

## 2.3 Conventions

- A `cnf` directory identifies a bnd workspace.
- Each workspace project normally has a `bnd.bnd`.
- `.bndrun` files describe resolve, launch, test, and export configurations.
- Bundle symbolic names and package capabilities, not Maven coordinates alone,
  identify runtime OSGi modules.
- Build-tool adapters should remain thin relative to bndlib.
- Exported Java packages carry semantic versions.

## 2.4 Compatibility constraints

The project explicitly values backward compatibility. Architectural changes
must account for:

- persisted bnd files and macros;
- public Java APIs and exported package versions;
- Gradle plugin IDs and task behavior;
- Maven coordinates, goals, and lifecycle integration;
- Eclipse extension IDs, preferences, workspace metadata, and launch files;
- repository index formats and `.bndrun` behavior.

[Back to index]({{ '/development/architecture/' | relative_url }})

