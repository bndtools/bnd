---
layout: bnd
title: Architecture Decisions
parent: Architecture (arc42)
grand_parent: Developing bnd and Bndtools
nav_order: 9
---

# 9. Architecture Decisions

This section records decisions visible in the current architecture. It does not
claim to replace historical maintainer discussions.

## ADR-001: Keep bndlib independent of host tools

**Decision:** Core bundle analysis/building and workspace concepts live in Java
libraries that do not depend on Eclipse, Maven, or Gradle.

**Rationale:** Multiple front ends need identical semantics; embeddability is a
primary product capability.

**Consequences:** Adapters translate host models and may need compatibility
layers, but core fixes benefit every host.

## ADR-002: Derive metadata from bytecode

**Decision:** Analyze compiled classes and merge results with concise,
pattern-based instructions rather than requiring complete handwritten
manifests.

**Rationale:** Class dependencies are authoritative and handwritten duplication
is error-prone.

**Consequences:** Correct class-file parsing and instruction ordering are
critical; unusual dynamic dependencies require explicit instructions.

## ADR-003: Use package/capability semantics

**Decision:** Model runtime compatibility with OSGi packages, capabilities, and
requirements.

**Rationale:** Build-tool module dependencies do not express OSGi runtime
wiring.

**Consequences:** Users must understand distinction between Maven coordinates,
bundle identity, and package capabilities.

## ADR-004: Separate desired and resolved runtime state

**Decision:** `.bndrun` stores desired `-runrequires` and can persist selected
`-runbundles`.

**Rationale:** Requirements remain intention-revealing while resolved bundles
provide repeatable deployment.

**Consequences:** Resolution must be rerun after repository or requirement
changes; generated selections should be reviewed.

## ADR-005: Support both bnd workspaces and conventional builds

**Decision:** Provide a complete bnd workspace model plus adapters for ordinary
Gradle and Maven projects.

**Rationale:** Cohesive bnd workspaces optimize OSGi development, while many
users must remain in established build structures.

**Consequences:** Gradle has separate builder and workspace modes; Maven plugins
cover operations as goals instead of reproducing the full workspace UX.

## ADR-006: Build Maven plugins with Maven

**Decision:** Use a Maven reactor for Maven plugin projects after core bnd
artifacts are built.

**Rationale:** Maven's plugin tooling and integration testing work best inside
Maven.

**Consequences:** Full builds are multi-stage and require local availability of
matching bnd artifacts.

## ADR-007: Build Bndtools as Eclipse OSGi bundles

**Decision:** Organize IDE capabilities as Eclipse plug-ins with public APIs,
extension points, and OSGi services.

**Rationale:** Native integration is required for editors, builders, launch,
debug, p2, resources, jobs, and markers.

**Consequences:** Eclipse API compatibility and bundle metadata are additional
constraints; headless core behavior must stay outside UI bundles.

## ADR-008: Make launch and test pluggable

**Decision:** Select launchers and testers through run-path/runtime components.

**Rationale:** Local, remote, continuous, and build-time scenarios need
different execution strategies.

**Consequences:** Protocol and result compatibility matter across independently
versioned host and runtime components.

## ADR-009: Enforce semantic compatibility with own tooling

**Decision:** Version exported packages and use bnd baseline comparison during
development/release.

**Rationale:** The ecosystem exposes broad APIs consumed by many external
builds.

**Consequences:** API changes require deliberate version changes and migration
planning.

## ADR-010: Consolidate implementation in the main repository

**Decision:** Keep current bnd, Bndtools, build plugins, docs, and runtime
components together; archive superseded repositories.

**Rationale:** Atomic changes across core and adapters reduce version skew and
coordination cost.

**Consequences:** The main repository is large and its validation/build pipeline
is complex; delivery repositories still remain separate.

[Back to index]({{ '/development/architecture/' | relative_url }})

