---
layout: bnd
title: Deployment View
parent: Architecture (arc42)
grand_parent: Developing bnd and Bndtools
nav_order: 7
---

# 7. Deployment View

## 7.1 Development and build nodes

```mermaid
flowchart LR
    subgraph Workstation["Developer workstation"]
      JDK[JDK 17+]
      Eclipse[Eclipse + Bndtools]
      Gradle[Gradle wrapper]
      Maven[Maven wrapper]
      CLI[bnd CLI]
      Workspace[Git checkout / bnd workspace]
    end

    subgraph Services["External services"]
      MavenCentral[Maven Central]
      PluginPortal[Gradle Plugin Portal]
      JFrog[JFrog release/snapshot repositories]
      P2[Eclipse p2 sites]
      GitHub[GitHub repositories, Pages, Actions]
    end

    subgraph Target["Runtime target"]
      VM[JVM]
      Framework[OSGi framework]
      Bundles[Resolved application bundles]
    end

    Workspace --> Eclipse
    Workspace --> Gradle
    Workspace --> Maven
    Workspace --> CLI
    Eclipse <--> Services
    Gradle <--> Services
    Maven <--> Services
    CLI <--> Services
    Eclipse --> VM
    CLI --> VM
    Gradle --> VM
    Maven --> VM
    VM --> Framework --> Bundles
```

## 7.2 Published units

| Unit | Channel | Consumer |
|---|---|---|
| bnd Java libraries and CLI | Maven Central and JFrog | Applications, adapters, CLI users |
| Gradle plugin artifact and marker plugins | Gradle Plugin Portal/Maven repositories | Gradle builds |
| Maven plugins | Maven Central/JFrog plugin repositories | Maven builds |
| Bndtools Eclipse features/bundles | p2 update sites | Eclipse installations |
| Snapshot and release-candidate artifacts | JFrog | Early adopters and CI |
| Documentation | bnd site and GitHub Pages Bndtools site | Users and contributors |
| Workspace/project templates | GitHub/template repositories and Bndtools | New projects/workspaces |

## 7.3 Main repository build deployment

```mermaid
flowchart TB
    Source[Source checkout]
    CoreBuild[Gradle workspace build]
    LocalRepo[Local Maven repository / dist outputs]
    GradleBuild[Included Gradle plugin build]
    MavenBuild[Maven plugin reactor]
    Publish[Publication tasks]

    Source --> CoreBuild
    CoreBuild --> LocalRepo
    CoreBuild --> GradleBuild
    LocalRepo --> MavenBuild
    GradleBuild --> Publish
    MavenBuild --> Publish
    CoreBuild --> Publish
```

Core workspace artifacts must exist before Maven plugins can compile and test.
Release workflows then publish library/plugin artifacts and assemble Eclipse
update-site content.

## 7.4 Runtime deployment variants

| Variant | Description |
|---|---|
| Local development launch | Launcher creates a child JVM, starts an OSGi framework, installs run bundles, and synchronizes changes |
| Remote launch | Remote launcher/agent deploys and updates bundles in another JVM or host |
| Executable export | `.bndrun` is exported into an executable JAR or another supported distribution |
| Repository distribution | Bundles are published with Maven metadata and/or an OSGi repository index |
| Eclipse installation | p2 installs Bndtools features and bundles into Eclipse |

## 7.5 Operational assumptions

- Artifact repositories and update sites are external dependencies.
- Local caches improve performance but must not replace explicit repository
  configuration.
- Credentials for publication belong in CI/user settings, never source.
- Launch storage and generated build directories are disposable.
- `.bndrun` files and resolved bundle lists are source-controlled when
  reproducibility is required.

## 7.6 Pending editor and agent deployment

The VS Code extension is maintained outside the main bnd repository. Its bundled
server/CLI versions and source provenance are recorded in
`server/bundled-jars.json` in that repository; extension version alone does not
identify the server source snapshot.

The reviewed extension package is `0.28.0`; bundled LSP and JDT-LS adapter
metadata records `7.6.0.202610301056` from `007598d18`, not the newer reviewed
`fea-bnd-ls` HEAD. The extension working tree also contains uncommitted Eclipse
coexistence support with twin output directories (`bin.vscode`,
`bin_test.vscode`, and `generated.vscode`) and conflict detection. This is
client-side integration, not an LSP or MCP protocol capability.

The LSP server runs as a Java process over stdio or TCP. Java integration uses
the JDT-LS adapter; the extension's fallback Node server provides only limited
editing support when Java is unavailable. MCP runs as a separate stdio server
and owns the child JVMs started by its run tools.

`fea-bnd-ls` and `fea-mcp` are **pending integration**. Do not infer Maven Central,
p2, or master availability from a locally built or bundled server JAR. Consult
the [Tooling Feature Matrix]({{ '/development/feature-matrix.html' | relative_url }})
for reviewed revisions and runtime constraints.

[Back to index]({{ '/development/architecture/' | relative_url }})
