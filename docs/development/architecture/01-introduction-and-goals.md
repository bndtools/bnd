---
layout: bnd
title: Introduction and Goals
parent: Architecture (arc42)
grand_parent: Developing bnd and Bndtools
nav_order: 1
---

# 1. Introduction and Goals

## 1.1 Requirements overview

bnd is an embeddable toolchain for creating, analyzing, resolving, launching,
testing, and maintaining OSGi bundles. Its central capability derives OSGi
metadata from Java bytecode and combines that analysis with concise user
instructions. Bndtools supplies Eclipse-based interaction with the same model.
Gradle, Maven, Ant, and CLI adapters expose bnd capabilities in other workflows.

The system must:

1. Generate correct bundle content and OSGi metadata from classes, resources,
   dependencies, and bnd instructions.
2. Provide an IDE- and build-tool-independent workspace/project model.
3. Resolve runtime requirements against OSGi repositories.
4. Launch, export, and continuously update OSGi runtimes.
5. Run ordinary JVM tests and in-framework OSGi tests.
6. Compare APIs and enforce OSGi semantic versioning through baselining.
7. Offer equivalent core behavior through CLI, Eclipse, Gradle, Maven, and Ant.
8. Preserve compatibility for public APIs and established build behavior.

## 1.2 Quality goals

| Priority | Goal | Architectural response |
|---|---|---|
| 1 | Correctness | Central bytecode/manifest model, verification, resolver integration, and large automated test suites |
| 2 | Backward compatibility | Versioned exported packages, baselining, conservative public API changes |
| 3 | Tool consistency | Thin adapters delegate to shared bndlib and workspace models |
| 4 | Extensibility | Plugin services for repositories, resolvers, launchers, exporters, generators, and reporters |
| 5 | Reproducibility | Declarative instructions, explicit repositories, pinned run bundles, wrappers, and controlled publication |
| 6 | Developer feedback | Incremental builds, Eclipse markers, continuous launch/test synchronization, rich diagnostics |
| 7 | Embeddability | Core Java libraries remain usable without Eclipse, Maven, or Gradle runtime coupling |

## 1.3 Stakeholders

| Stakeholder | Expectations |
|---|---|
| OSGi application developer | Small declarative configuration, correct manifests, fast feedback |
| Library/bundle maintainer | API comparison, semantic-version guidance, reproducible artifacts |
| Eclipse/Bndtools user | Editors, validation, repositories, resolution, launch, debug, and test UX |
| Gradle user | Native tasks and plugins for workspace and ordinary Gradle builds |
| Maven user | Lifecycle-bound goals for bundle generation, baseline, resolve, export, run, test, index, report, and generate |
| Build/release engineer | Headless operation, stable repositories, CI diagnostics, signed/versioned releases |
| Tool integrator | Stable Java APIs and extension points around bndlib |
| Project maintainer | One coherent model, manageable compatibility burden, testable adapters |

## 1.4 Out of scope

- Implementing an OSGi framework.
- Replacing Java compilers, Maven, Gradle, or Eclipse.
- Hosting user applications at runtime.
- Defining OSGi specifications.
- Describing every instruction, CLI command, editor, or plugin goal.

[Back to index]({{ '/development/architecture/' | relative_url }})

