---
title: Bnd Language Server
layout: bnd
summary: Language features and workspace operations for bnd files, plus a Java bridge for JDT LS integrations.
parent: Tools bound to bnd
---

The `biz.aQute.bnd.lsp` project provides a Language Server Protocol (LSP) server for bnd files. It uses the bnd workspace model to provide editor features and workspace operations.

## Editor Features

For open `.bnd` and `.bndrun` files, the server provides:

- Completion and hover information for bnd headers and clauses.
- Semantic tokens and bnd diagnostics.
- Document symbols and folding ranges.
- Go to definition for included files and projects referenced by `-buildpath`.
- A **Resolve Runbundles** code action for `.bndrun` files.

The server uses full document synchronization. Document formatting is not provided.

## Workspace Commands

The server registers these LSP execute-command identifiers:

| Identifier | Purpose |
| --- | --- |
| `bnd.build.project` | Build the project containing the supplied file URI. |
| `bnd.build.workspace` | Build all projects in the workspace containing the supplied file URI. |
| `bnd.resolve` | Resolve the supplied `.bndrun` file URI and update its runbundles. |
| `bnd.macro.expand` | Expand a macro expression, optionally using a file URI as context. |
| `bnd.repo.list` | List repositories for the workspace containing an optional file URI. |
| `bnd.jar.print` | Read manifest headers from the supplied JAR path. |
| `bnd.properties.effective` | Evaluate effective properties for a `.bnd` or `.bndrun` file. |

Effective-properties evaluation requires a trusted workspace. It uses the open document text when available and accepts a document version so clients can detect stale results. Its request can select expanded values and merged instructions/headers.

## JDT LS Bridge

The `org.bndtools.jdtls` project exports a headless Java API for JDT LS integrations. `BndJdtLsBridge` detects bnd workspaces and exposes project and workspace metadata, resolved build/test classpaths, and package names under source and test roots. The bridge is a library; it does not itself register as a JDT LS extension or provide an LSP client.

The separate `org.bndtools.jdtls.adapter` bundle registers a native JDT LS project importer, classpath-container initializer and build-support extension. It imports bnd projects before Gradle, maps source/test roots and outputs, resolves build/test dependencies and refreshes them after configuration changes. It requires JDT LS 1.61 or later running on Java 21 or later. Build it using Java 21 or later with `JDT_LS_CORE_JAR` set to the installed `org.eclipse.jdt.ls.core_*.jar`, then contribute the resulting bundle through a VS Code extension's `contributes.javaExtensions`. Imported project metadata is owned by bnd rather than Gradle/Buildship. Source and output directories outside a project are currently unsupported.
