# Bndtools Development: Tips and Tricks

Development notes have moved to [docs/development/development-notes.md](docs/development/development-notes.md).

See the [contributor documentation](docs/development/index.md) for architecture,
documentation development, and maintainer procedures.

## Headless Language Server and VS Code Integration

`biz.aQute.bnd.lsp` provides the Java bnd language server used by the
[bnd VS Code extension](https://github.com/peterkir/vscode-bnd-plugin).
Build and test the server from this repository root with Java 17 or later:

```bash
./gradlew :biz.aQute.bnd.lsp:test :biz.aQute.bnd.lsp:jar
```

The executable JAR is `biz.aQute.bnd.lsp/generated/biz.aQute.bnd.lsp.jar`.
Configure the extension's `bnd.server.mode` as `java` and `bnd.server.jar` as
the absolute path to this file, then run **Bnd: Restart Language Server**.
The Node fallback does not implement the repository, JAR print-text, or
resolution-analysis commands below. Clients should check the server's
`executeCommandProvider.commands` capability before using them.

### Repository Commands

The `bnd.repositories.*` and `bnd.workspace.offline` commands use
`workspace/executeCommand` with one object in `arguments`. Every request
includes `workspace`, a file URI for the directory containing `cnf/build.bnd`.
Repository-specific requests use `repo` (the index from the list response)
and `repoName`. A mismatched name is rejected; refresh the repository list
after configuration changes rather than reusing stale indexes.

| Command | Additional request fields | Response |
| --- | --- | --- |
| `bnd.repositories.list` | None | `workspace`, `offline`, `repositories[]` |
| `bnd.repositories.bundles` | `repo`, `repoName`, optional `filter` | `bundles[]`, `features[]` for P2 repositories |
| `bnd.repositories.versions` | `repo`, `repoName`, `bsn` | `versions[]`, newest first |
| `bnd.repositories.feature` | `repo`, `repoName`, `id`, `version` | Feature metadata, `plugins[]`, `includes[]`, `requires[]` |
| `bnd.repositories.get` | `repo`, `repoName`, `bsn`, `version` | Local `file` path and `uri` |
| `bnd.repositories.search` | `namespace`, LDAP `filter` | `results[]` from plugins implementing the OSGi Repository API |
| `bnd.repositories.listActions` | `repo`, `repoName`, optional `bsn`, `version` | `actions[]` containing plugin action labels |
| `bnd.repositories.runAction` | `repo`, `repoName`, `label`, optional `bsn`, `version` | Runs the selected plugin action |
| `bnd.repositories.reload` | Optional `repo`, `repoName` | `refreshed[]` |
| `bnd.repositories.put` | `repo`, `repoName`, `files[]` of local file URIs | `added[]` |
| `bnd.repositories.fetch` | `repo`, `repoName`, optional `bsn`, `version` | `downloaded`, `errors[]` |
| `bnd.workspace.offline` | Optional `offline` boolean | Current `offline` state |

The Workspace repository is listed first and groups bundle symbolic names by
project. Plugin repositories expose status, location, tags, and flags for
writable, remote, refreshable, actionable, searchable, and P2 capabilities.
P2 repositories additionally expose feature contents. Bundle filters allow
`*` and `?` wildcards with substring matching. Advanced search builds an OSGi
requirement from the requested namespace and LDAP filter and finds providers
only in repository plugins implementing `org.osgi.service.repository.Repository`.

Initialize with `initializationOptions.workspaceTrusted: true` to enable
repository commands. Loading plugins, downloading artifacts, writing to
repositories, and running `Actionable` actions can execute workspace or plugin
code; trust is not a sandbox. Offline mode is a workspace runtime setting.
Errors are returned as objects containing `error`; downloads additionally
report per-artifact failures in `errors[]`.

### JAR Print Text

`bnd.jar.printText` accepts `{ "uri": "file:///path/to/bundle.jar" }` as its
single argument and returns `{ "text": "..." }`. It uses
`JarPrinter.doPrint(jar, -1, false, false)` for the full report: manifest,
imports/exports, capabilities, components, metatype, API, uses, and entries.
This is separate from `bnd.jar.print`, which returns structured manifest data.
It requires an existing local `.jar` file and does not change the archive.
The extension's Tree page reads ZIP entries locally and remains usable without
this command; only the Print page requires the Java server.

### Resource Requirement and Capability Analysis

`bnd.resolution.analyze` accepts one object with `uris`, a list of 1-100 saved
local `.bnd` or `.jar` file URIs. It requires
`initializationOptions.workspaceTrusted: true`. JARs are read with
`ResourceBuilder`; `.bnd` files must belong to a bnd project and are analyzed
through its builder. For `bnd.bnd`, only the first project sub-builder is used;
select individual sub-bundle definitions or generated JARs to analyze the others.
Analysis can initialize workspace plugins and build bundle content, but does
not edit the source file. Unsaved editor changes are not included.

The response contains `resources[]` (absolute paths), `requirements[]`, and
`capabilities[]`. Each row includes `source`, `namespace`, `attributes`, and
`directives`; requirements also include `optional` and `resolved` booleans.
A requirement is marked `resolved` when a selected capability has the same
namespace and matches its filter. Missing, malformed, or unsupported filters
remain unresolved. This is selected-resource matching, not full OSGi resolution:
it does not compute consistent framework wiring or update `-runbundles`.
Use the LSP command `bnd.resolve` for `.bndrun` resolution instead.

In VS Code, selecting repository bundles or versions replaces the Resolution
Panel's resource list. Bundles use their newest available version. Dropping
repository entries or local `.bnd`/`.jar` files, or invoking **Analyze in
Resolution View**, adds resources. Resource choices persist per VS Code
workspace. Filters support multiple terms and wildcards, hiding optional
requirements, and showing only unmatched requirements.

### Native Java Import and Validation

`org.bndtools.jdtls.adapter` integrates bnd project import into Red Hat Java's
JDT LS, independently of the bnd language server. It requires JDT LS 1.61 or
later (Red Hat Java 1.56 or later) and Java 21 or later. It imports source/test
roots, separate outputs, compiler settings, JRE containers, and build/test
dependencies ahead of Gradle. Do not let Buildship manage the same projects.
Source and output directories must be inside their project directory.

Saved `.bnd` and `.mvn` changes, including `cnf` configuration, trigger model
updates subject to `java.configuration.updateBuildConfiguration`. In Red Hat
Java 1.56, **Java: Reload Projects** (`java.projectConfiguration.update`)
requests an update without clearing the workspace. After upgrading from
unmanaged projects, use **Java: Clean Java Language Server Workspace** and
reimport. Runtime names in `java.configuration.runtimes` must be execution
environment names such as `JavaSE-21`, not installation folder names.

Before testing the extension against server changes, update its bundled
`server/biz.aQute.bnd.lsp.jar` with the generated JAR, then run `npm test` in
the extension checkout with `BND_SOURCE_REPO` pointing to this repository.
That suite checks the real client/server transport, repository commands,
JAR print text, and resource analysis. `npm run test:jdtls` in the extension
checkout separately validates native Java import and live classpath refresh
in an isolated JDT LS; see its development guide for runtime variables.

## Unit Tests

See [Unit Tests](docs/development/development-notes.md#unit-tests).

## Temporary folders

See [Temporary folders](docs/development/development-notes.md#temporary-folders).

## Adding Error/Warning Markers

See [Adding Error/Warning Markers](docs/development/development-notes.md#adding-errorwarning-markers).

## Using the `ExtensionFacade`

See [Using the ExtensionFacade](docs/development/development-notes.md#using-the-extensionfacade).

## Generating JDK properties files

See [Generating JDK properties files](docs/development/development-notes.md#generating-jdk-properties-files).

## Macro Documentation Testing

See [Macro Documentation Testing](docs/development/development-notes.md#macro-documentation-testing).

## Running builds with local Bnd Gradle Plugins

See [Running builds with local Bnd Gradle Plugins](docs/development/development-notes.md#running-builds-with-local-bnd-gradle-plugins).