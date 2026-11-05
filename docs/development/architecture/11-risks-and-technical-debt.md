---
layout: bnd
title: Risks and Technical Debt
parent: Architecture (arc42)
grand_parent: Developing bnd and Bndtools
nav_order: 11
---

# 11. Risks and Technical Debt

| Risk or debt | Impact | Current mitigation / recommended action |
|---|---|---|
| Very broad feature surface and long compatibility history | Slow changes, hidden coupling, regression risk | Keep adapters thin; use targeted plus full suites; baseline public APIs |
| Main repository combines core, IDE, build plugins, runtimes, and docs | Long builds and contributor learning curve | Preserve module boundaries and staged validation; maintain architecture map |
| Different host lifecycle semantics | Behavior drift between Eclipse, Gradle, Maven, Ant, and CLI | Define behavior in bndlib; add cross-adapter conformance tests |
| Gradle workspace mode cannot use configuration cache | Slower large builds and friction with modern Gradle expectations | Document limitation; reduce execution-time model access if model serialization becomes feasible |
| Legacy APIs and commands remain for compatibility | Maintenance cost and confusing choices | Deprecate explicitly, publish migration paths, remove only on major versions |
| Legacy `biz.aQute.tester` pending removal | Migration burden for JUnit 3/4-specific behavior | Promote JUnit Platform tester and document unsupported compatibility features |
| Repository/network diversity | Non-reproducible or unavailable builds | Pin versions, record repositories, cache safely, verify artifacts and indexes |
| Macro/plugin execution in builds | Untrusted configuration can execute code or access environment | Treat builds as code execution; isolate CI for untrusted contributions |
| Remote launcher/agent capability | Accidental exposure or unauthorized runtime modification | Bind narrowly, authenticate where available, restrict network access |
| Eclipse API and p2 compatibility matrix | Installation and runtime failures across Eclipse releases | Test supported release trains and keep p2 metadata explicit |
| Documentation split across repositories | Stale or contradictory guidance | Identify source of truth by topic and cross-link versioned docs |
| Active templates can lag core/plugin versions | New workspaces start with obsolete constraints | Automate template compatibility checks and release updates |
| Historical repositories remain visible | Users may choose obsolete code or endpoints | Keep archived status/descriptions clear and link to successors |
| Multi-stage Gradle/Maven publication | Version skew and partial releases | Use one release version, ordered pipeline, and post-publication verification |
| Dynamic OSGi behavior is difficult to diagnose | Runtime-only failures and intermittent service availability | Preserve resolver traces, service diagnostics, launch tracing, and reproducible run definitions |

## 11.1 Highest-priority improvement areas

1. Maintain conformance tests that run representative bundle, resolve, and
   baseline scenarios through more than one adapter.
2. Complete migration guidance and compatibility tests for removal of legacy
   tester components.
3. Automate checks between current plugin versions, templates, website
   examples, and p2 release metadata.
4. Make repository provenance, checksum, and trust behavior easy to audit.
5. Keep this architecture document current when modules move between the main
   and supporting repositories.

[Back to index]({{ '/development/architecture/' | relative_url }})

