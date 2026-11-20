---
layout: bnd
title: Runtime View
parent: Architecture (arc42)
grand_parent: Developing bnd and Bndtools
nav_order: 6
---

# 6. Runtime View

## 6.1 Build a bundle

```mermaid
sequenceDiagram
    actor User
    participant Host as CLI / Gradle / Maven / Eclipse
    participant Model as bnd workspace or builder
    participant Analyzer as bytecode analyzer
    participant Builder as JAR/manifest builder
    participant Verifier as verifier

    User->>Host: Request build
    Host->>Model: Supply project, classpath, and instructions
    Model->>Analyzer: Analyze classes and annotations
    Analyzer-->>Model: Package references and capabilities
    Model->>Builder: Merge analysis, instructions, and resources
    Builder->>Verifier: Verify bundle and manifest
    Verifier-->>Host: Diagnostics and artifact
    Host-->>User: Bundle or build failure
```

Key behavior:

- ordered instructions select private/exported content;
- missing package references become candidate imports;
- macros and host properties are evaluated through a processor hierarchy;
- verification occurs before successful publication;
- Eclipse converts diagnostics into resource markers; headless hosts report
  them through native logging/failure mechanisms.

## 6.2 Resolve a `.bndrun`

```mermaid
sequenceDiagram
    actor User
    participant Adapter
    participant RunModel as bndrun model
    participant Resolver
    participant Repositories

    User->>Adapter: Resolve run description
    Adapter->>RunModel: Load inherited properties and -runrequires
    RunModel->>Repositories: Collect framework and resource capabilities
    RunModel->>Resolver: Submit requirements and repository context
    Resolver->>Repositories: Query matching capabilities
    Resolver-->>RunModel: Resolution or diagnostic chain
    RunModel-->>Adapter: Selected bundles
    Adapter-->>User: Display or persist -runbundles
```

Resolution separates desired capabilities (`-runrequires`) from selected,
versioned artifacts (`-runbundles`). Writing the result back creates a
reviewable and reproducible runtime definition.

## 6.3 Launch and synchronize

```mermaid
sequenceDiagram
    actor User
    participant Host as Eclipse / CLI / build plugin
    participant LauncherPlugin as launcher plugin
    participant VM as local or remote VM
    participant Framework as OSGi framework

    User->>Host: Run .bndrun
    Host->>LauncherPlugin: Launch with run path, framework, bundles, properties
    LauncherPlugin->>VM: Start or connect
    VM->>Framework: Create and initialize
    LauncherPlugin->>Framework: Install and start run bundles
    Host-->>LauncherPlugin: Project or configuration changed
    LauncherPlugin->>Framework: Update bundles and settings
    User->>Host: Stop
    Host->>LauncherPlugin: Cancel/terminate
```

The default launcher starts a local VM. A remote launcher can connect to a
remote agent. Launcher selection is pluggable through a `Launcher-Plugin`
header on the run path.

## 6.4 OSGi test execution

```mermaid
sequenceDiagram
    participant Build as Build tool or Eclipse
    participant Launcher
    participant Framework as OSGi framework
    participant Tester as JUnit Platform tester
    participant Tests as Test bundles

    Build->>Launcher: Start test run
    Launcher->>Framework: Install/start framework and bundles
    Framework->>Tester: Activate tester
    Tester->>Framework: Discover TestEngine services and test bundles
    Tester->>Tests: Execute selected tests
    Tests-->>Tester: Results and output
    Tester-->>Build: Status and XML/IDE result protocol
```

The preferred tester is `biz.aQute.tester.junit-platform`. The legacy
`biz.aQute.tester` remains for compatibility but is deprecated.

## 6.5 Eclipse incremental build

1. Eclipse reports resource changes to the Bndtools builder.
2. Bndtools maps the Eclipse project to the bnd workspace/project model.
3. bnd computes affected outputs and diagnostics.
4. Generated bundles and model state are refreshed.
5. Diagnostic locations are converted into Eclipse markers.
6. Active launch sessions can receive updated bundles.

## 6.6 Baseline comparison

1. Adapter builds the current bundle.
2. Repository lookup obtains the configured previous bundle.
3. bnd compares exported APIs and relevant implementation changes.
4. Delta severity is mapped to required package and bundle version changes.
5. The host reports violations and versioning advice.

## 6.7 Pending LSP and MCP workflows

These workflows describe `fea-bnd-ls` and `fea-mcp`, not master:

- The editor sends LSP requests for editing features and `bnd.*` execute commands
  for workspace operations. Diagnostics are published back to the editor.
- `bnd.resolve` can write `-runbundles` or return a dry-run result with
  `writeChanges=false`; server support does not imply a client dry-run control.
- `bnd.launch.prepare` returns a launch configuration for run or OSGi test mode.
  The client starts/debugs the JVM and later calls `bnd.launch.dispose` to release
  the retained server-side launch session.
- MCP agents invoke `bnd_build` per project and `bnd_resolve` with an explicit
  write-back choice. MCP does not expose OSGi test execution.
- `bnd_run_start` creates a managed child JVM identified by `runId`;
  `bnd_run_status` reports its state/output, and `bnd_run_stop` stops it.
  Server shutdown cleans up managed sessions.
- The MCP working tree adds readiness selection (`http`, `console` with
  `readyPattern`, or `none`). This is **uncommitted behavior**, not a released
  readiness contract. `bnd_http_get` supports loopback HTTP smoke checks.

Exact inventories and reviewed revisions are in the
[Tooling Feature Matrix]({{ '/development/feature-matrix.html' | relative_url }}).

[Back to index]({{ '/development/architecture/' | relative_url }})
