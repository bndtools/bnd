---
layout: bnd
title: Solution Strategy
parent: Architecture (arc42)
grand_parent: Developing bnd and Bndtools
nav_order: 4
---

# 4. Solution Strategy

## 4.1 Core strategy

Build one reusable semantic engine, then adapt it to multiple hosts.

1. **Analyze instead of duplicate:** derive imports, exports, capabilities, and
   other metadata from bytecode and annotations.
2. **Use declarative instructions:** merge analysis with ordered bnd
   instructions and macros.
3. **Model workspace independently:** keep `Workspace`, `Project`, builders,
   repositories, and plugins independent of Eclipse, Maven, and Gradle.
4. **Delegate from adapters:** CLI, IDE, and build-tool components translate
   host concepts into shared bnd operations.
5. **Use OSGi semantics end to end:** capabilities and requirements connect
   build, repository, resolve, launch, and test flows.
6. **Make advanced operations pluggable:** repository, resolver, launcher,
   exporter, generator, and report services can vary without changing the core.

## 4.2 Mapping goals to approaches

| Goal | Approach |
|---|---|
| Correct metadata | Bytecode analysis plus manifest verification |
| Consistent tools | Shared bndlib/workspace models behind adapters |
| Reproducible runtime | Resolve requirements to explicit `-runbundles` |
| Fast IDE feedback | Incremental workspace build and resource markers |
| Runtime realism in tests | Launch tests inside an OSGi framework |
| Compatibility | Package versions, API diff, and baseline enforcement |
| Extensibility | Service interfaces, plugins, Eclipse extension points |
| Broad adoption | CLI, Eclipse, Gradle, Maven, Ant, and embeddable Java APIs |

## 4.3 Build strategy

The main build has three layers:

1. The Gradle wrapper builds bnd workspace projects.
2. The included `gradle-plugins` build builds and tests Gradle plugins.
3. The Maven wrapper builds Maven plugins after required bnd artifacts are
   available locally.

This separation accepts host-tool constraints while keeping plugin behavior
anchored in common libraries.

## 4.4 Repository strategy

bnd abstracts artifact sources behind repository plugins. A workspace can
combine local workspace outputs, indexed file repositories, Maven-backed
repositories, and remote OSGi repository indexes. Resolution consumes OSGi
resources and capabilities rather than relying solely on build-tool dependency
graphs.

## 4.5 Product strategy

- **bnd** supplies libraries, CLI, workspace model, repositories, resolver,
  launcher/tester, and build plugins.
- **Bndtools** supplies Eclipse UX around those services.
- **Template repositories** supply starting configurations rather than another
  implementation of the toolchain.
- **Website and p2 repositories** separate content delivery from core source.

[Back to index]({{ '/development/architecture/' | relative_url }})

