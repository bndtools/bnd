---
layout: bnd
title: Cross-cutting Concepts
parent: Architecture (arc42)
grand_parent: Developing bnd and Bndtools
nav_order: 8
---

# 8. Cross-cutting Concepts

## 8.1 Configuration and macro processing

bnd treats configuration as hierarchical properties processed by a macro
engine. Workspace configuration, project configuration, included files,
profiles, and adapter-supplied values form an evaluation context. Uppercase
properties generally represent manifest headers; lowercase or dash-prefixed
properties configure bnd behavior.

Architectural consequences:

- adapters must preserve property precedence and evaluation timing;
- diagnostics should identify the originating file and instruction;
- configuration is data and can be reused without host-specific code;
- unrestricted macros and external inputs require care in untrusted builds.

## 8.2 OSGi resources, capabilities, and requirements

Bundles and repository entries are modeled as resources that provide
capabilities and declare requirements. This model unifies:

- package imports and exports;
- framework and execution-environment selection;
- repository search;
- application resolution;
- runtime assembly.

Build-tool dependency graphs remain useful for compilation, but they do not
replace OSGi resolution.

## 8.3 Versioning and baselining

Exported packages have semantic versions. API comparison computes changes
between current and prior bundles and recommends or enforces package/bundle
version increments. This protects both library consumers and adapters that
embed bnd.

## 8.4 Plugin architecture

Extension mechanisms exist at several levels:

| Level | Mechanism | Examples |
|---|---|---|
| bnd processor/workspace | Java plugins and service interfaces | repositories, lifecycle actions, progress, exporters |
| OSGi runtime | Services and Declarative Services | runtime collaboration and dynamic availability |
| Eclipse | Extension registry and OSGi services | editors, marker detail handlers, templates, views |
| Gradle | Plugin API and task types | builder/workspace/project plugins |
| Maven | Mojos and lifecycle bindings | build, baseline, resolve, test, export |

Extensions should depend on the narrowest stable API package available.

## 8.5 Error handling and diagnostics

- Core operations collect errors and warnings with optional structured
  locations/details.
- Build adapters convert errors into native build failures and logs.
- Bndtools creates precise Eclipse markers where location detail permits.
- Resolver failures should retain requirement chains and candidate rejection
  reasons.
- Failures must not be converted into success-shaped empty artifacts.

## 8.6 Concurrency and incremental operation

Eclipse jobs, Gradle workers/tasks, Maven reactor execution, repository access,
and file watchers can run concurrently. Shared workspace state and caches must
be synchronized or isolated. Incremental behavior relies on timestamps,
resource deltas, calculated dependencies, and stable outputs.

## 8.7 Test architecture

The repository combines:

- unit tests for parsers, models, analyzers, repositories, and utilities;
- integration tests over synthetic workspaces and artifacts;
- Gradle TestKit-style plugin scenarios;
- Maven plugin integration tests;
- Eclipse/Bndtools plug-in tests;
- launched-framework OSGi tests;
- API baselining and full rebuild checks.

JUnit Platform is the current direction. Temporary resources and network-facing
test servers are isolated in test support modules.

## 8.8 Security and trust boundaries

The toolchain processes third-party JARs, repository metadata, build scripts,
macros, templates, and remote content. Relevant controls include:

- HTTPS and repository provenance;
- digest/signature verification where supplied;
- bounded parsing of malformed archives/class files;
- no secrets in bnd files or generated manifests;
- explicit publication credentials outside source;
- awareness that Gradle/Maven builds and some bnd plugins/macros execute code;
- cautious remote launcher and agent exposure.

## 8.9 Logging and progress

Core APIs report diagnostics and progress without assuming one UI. Adapters map
them to console output, Maven/Gradle logging, Eclipse progress monitors, markers,
and test result views.

## 8.10 Documentation

The bnd site is maintained under `docs` in the main repository. Bndtools IDE
content is maintained in `bndtools.github.io`. READMEs near Gradle and Maven
plugins describe version-specific behavior. Architecture claims should link to
source or maintained documentation and include a snapshot date.

[Back to index]({{ '/development/architecture/' | relative_url }})

