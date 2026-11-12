---
name: update-lsp
description: Audit bnd language server (biz.aQute.bnd.lsp) parity against capabilities of bndlib, bnd CLI and Eclipse Bndtools, then close approved gaps. Use when checking LSP feature coverage, or after adding an instruction, macro, CLI command, quick fix or Bndtools UI feature that the LSP should mirror.
---

# update-lsp

Goal: a **parity matrix** proving every capability the repo offers to bnd users is either available through the language server or deliberately out of scope. Read [ARCHITECTURE.md](../../../biz.aQute.bnd.lsp/ARCHITECTURE.md) first; it maps LSP methods, commands and owning services.

## Steps

1. **Baseline the LSP surface.** Collect from source, not from docs:
   - capabilities and advertised command IDs in `BndLanguageServer.initialize`
   - command IDs in `Constants.java` and dispatch cases in `BndWorkspaceService.executeCommand`
   - diagnostic codes and quick fixes (`Constants.DIAGNOSTIC_CODE_*`, `BndQuickFixProvider`)
   - completion/hover sources in `BndCompletionProvider` and `BndHoverProvider` (generated from bndlib metadata or hard-coded lists)
   - tests under `biz.aQute.bnd.lsp/test`

   Done when each command ID is traced through constant, advertisement, dispatch and test; record every break in that chain as a **wiring gap**.

2. **Inventory capability sources.** Walk every source in the table below. Each source is independent; dispatch one `explore` subagent per source when the inventory is large, and ask each to return a list of `capability | evidence file:line`. Done when every row of the table has a complete list.

3. **Classify each capability** against the baseline:
   - `covered`: reachable through LSP, with test evidence
   - `partial`: reachable but missing arguments, file types, diagnostics or quick fixes the source offers
   - `missing`: editor-relevant and absent
   - `out-of-scope`: belongs to the client UI or build tool; state the reason (e.g. perspective, preference page, Gradle task graph)

   Done when every inventoried capability has exactly one status and evidence on both sides (source location, LSP location or "none").

4. **Check docs drift.** Compare findings with `ARCHITECTURE.md` (capability table, command table, known-limitations notes). List each stale statement.

5. **Report** the parity matrix grouped by source, with `missing`, `partial` and wiring gaps first, each with a one-line implementation sketch naming the owning LSP service. Then ask the user which gaps to close. Stop here if the request was audit only.

6. **Close approved gaps**, one gap per change, following the extension workflow in `ARCHITECTURE.md`:
   - reuse bndlib/bnd APIs that the source capability already calls; keep protocol code in the LSP service, bnd logic in a focused service
   - define trust requirement, argument/result contract and disposal at the command boundary
   - add focused tests plus a `BndLanguageServerTest` case when advertisement or routing changes
   - update `ARCHITECTURE.md` tables in the same change

   Validate with `./gradlew :biz.aQute.bnd.lsp:test :biz.aQute.bnd.lsp:jar`. Done when the build is green and re-running step 1 shows no wiring gaps for touched IDs.

## Capability sources

| Source | Where to look | LSP counterpart |
| --- | --- | --- |
| Instructions and headers | `biz.aQute.bndlib/src/aQute/bnd/help/Syntax.java`, `aQute.bnd.osgi.Constants`, `docs/_instructions`, `docs/_heads` | completion, hover, semantic tokens, diagnostics |
| Macros | `_name(String[])` methods on `Macro`, `Processor`, `Project`, `Workspace`, `Builder`, `Analyzer`; `docs/_macros` | macro completion, hover, `bnd.macro.expand` |
| bnd CLI commands | `_name` methods and nested `@Description` options in `biz.aQute.bnd/src/aQute/bnd/main/*.java`; `docs/_commands` | `workspace/executeCommand` IDs |
| Build errors with details | bndlib `SetLocation.details(...)` objects; `bndtools.core.buildErrorDetailsHandlers` handlers in `bndtools.core/_plugin.xml` | diagnostic codes with `data` and `BndQuickFixProvider` fixes |
| Eclipse marker and JDT quick fixes | `org.eclipse.ui.ide.markerResolution` and `org.eclipse.jdt.ui.quickFixProcessors` in `bndtools.*/_plugin.xml` | code actions |
| Eclipse editors | `bndtools.editor.BndEditor` pages and parts (`bndtools.core/src/bndtools/editor`): bundle, project, run, workspace config | document symbols, completion, code actions, resolve, launch |
| Eclipse views | Repositories, Resolution, Bundle Graph, JAR editor (`bndtools.jareditor`), Bndtools Explorer | `bnd.repositories.*`, `bnd.resolution.analyze`, `bnd.jar.print*` |
| Wizards and templates | `org.eclipse.ui.newWizards`, `importWizards`, `runExportWizards`, `org.bndtools.templating*`, `bndtools.core.launchTemplates` | `bnd.wizard.*` |
| Launch and test | `org.bndtools.launch`, `bndtools.core` launch configuration types and shortcuts | `bnd.launch.*` |
| Builder | `bndtools.builder` (incremental build, markers, baseline, classpath container) | build/clean/baseline commands, diagnostics |
| Other Bndtools bundles | `bndtools.m2e`, `bndtools.pde`, `bndtools.release`, `org.bndtools.remoteinstall`, `org.bndtools.p2`, `org.bndtools.jdtls*` | case by case; JDT LS bridge lives in `org.bndtools.jdtls` |

Gradle and Maven plugins drive builds outside the editor; classify them `out-of-scope` unless they expose a workspace operation that none of the sources above covers.
