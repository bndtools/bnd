---
name: sync-and-update-doc
description: 'Analyze bnd source code and sync or update documentation, arc42 architecture, and the tooling feature matrix. Use when checking documentation freshness, auditing feature parity, or documenting changes in master, fea-bnd-ls, fea-mcp, or the VS Code bnd extension, including unmerged and uncommitted changes.'
---

# Sync and update documentation

## Scope

Maintain `docs/`, especially `docs/development/feature-matrix.md` and all twelve
sections under `docs/development/architecture/`. Architecture lives only there;
do not recreate the intentionally removed root `arc42/` directory.

The matrix imported from `peterkir/vscode-bnd-plugin/docs/FEATURE-MATRIX.md`
is maintained here as a site page, not a blind copy on every run. Preserve its
seven columns, legend, feature groups, gaps, and command/tool inventories.
Do not modify the external extension repository unless explicitly requested.

## Procedure

1. **Establish evidence and scope.** Read repository instructions and
   `docs/Agents.md`, then inspect `git status --short`, branch, HEAD, and
   `git worktree list --porcelain`. Preserve all existing changes, including
   deletions. Read matrix provenance and architecture scope before editing.
   Use caller-provided source locations when supplied. Otherwise discover
   sibling worktrees named `fea-bnd-ls` and `fea-mcp`; the extension may be in a
   sibling owner checkout `peterkir/vscode-bnd-plugin`.
   On Windows use native absolute paths, not `/c/...` shell paths.
2. **Separate source snapshots.** Record branch, full HEAD, commit date, version,
   and relevant staged, unstaged, and untracked changes for each source.
   Compare feature branches against their merge base with local `master`
   (or `origin/master` if no local master exists). Do not fetch, switch branches,
   merge, reset, or discard changes. Inspect dirty source as part of the evidence,
   not just committed diffs. `fea-bnd-ls` and `fea-mcp` remain in documentation
   even before integration into master or the documentation checkout.
   Label capabilities as **pending integration**, and dirty-only behavior as
   **uncommitted**. A local or bundled artifact is not proof of a master release.
3. **Inventory actual implementations.** Trace registrations to handlers,
   request/result types, guards, and tests:
   - CLI command classes under `biz.aQute.bnd/src/aQute/bnd/main/`;
   - Eclipse extensions, builders, editors, launchers, and wizards in
     `bndtools.*` and `org.bndtools.*`;
   - plugin IDs, task types, goals, and lifecycle wiring in `gradle-plugins/`
     and `maven-plugins/`;
   - `biz.aQute.bnd.lsp` capabilities, `workspace/executeCommand` IDs,
     diagnostics, quick fixes, repositories, wizard, launch, and trust handling;
   - `org.bndtools.jdtls.adapter` importer, classpath, and build integration;
   - `biz.aQute.bnd.mcp` tool registrations, schemas, handlers, transport,
     diagnostics, readiness modes, HTTP restrictions, and process cleanup;
   - extension `package.json`, command registrations and callers, views,
     debug adapter, fallback server, and `server/bundled-jars.json`.
   Do not equate client command availability with server support, a server
   capability with a client UI, CLI terminal delegation with native support,
   advertised support with working handlers, or build support with OSGi testing.
4. **Check claims, not just diffs.** Compare every matrix row and appendix with
   evidence; confirm full/partial/missing/not-applicable ratings and named IDs.
   Verify defaults, dry-run/write-back, output shapes, trust gates, launch
   lifecycle, and runtime requirements. Check Gradle builder and workspace
   configuration-cache support separately. Tests corroborate behavior but
   documentation alone does not prove implementation.
   Record source paths and lines or commit-pinned source links for changed claims.
   Missing sources mean **not verified**, never automatically **missing**.
   If a required checkout/ref is unavailable, report the blocker and request
   its location; do not remove pending columns or invent capabilities.
5. **Update coherent documentation.** Apply supported corrections to matrix,
   source provenance, gaps, and inventories. Retain original import attribution.
   Update affected arc42 sections (context, building blocks, runtime, deployment,
   trust, quality, risks, glossary) and nearby command/instruction/macro guides.
   Keep master facts distinct from pending branch facts in prose and diagrams.
   Update review dates only for reviewed scope; retain explicit unverified areas.
   Keep one canonical architecture tree and link matrix from contributor and
   architecture indexes. Do not overwrite generated reference pages manually;
   use the repository's generation/extension mechanism where appropriate.
6. **Validate.** Follow `docs/development/documentation.md`: prefer Docker,
   falling back to existing Ruby/Bundler when Docker is unavailable. Build
   normal and `/releases/test` variants to explicit destinations outside the
   source tree. Check frontmatter, navigation, search inclusion, seven-column
   tables, internal links, release-prefixed links, and Mermaid rendering.
   Use existing targeted source tests when a changed behavior claim needs
   execution evidence; do not run full workspace builds for prose-only edits.
   Never claim browser rendering or source tests passed unless actually run.
   Report missing tooling or failed checks explicitly.
7. **Report.** Summarize corrected claims, files changed, snapshots reviewed,
   pending/uncommitted features, checks run, and unresolved verification gaps.
   Inspect final diff for unrelated edits and confirm root `arc42/` stays absent.
   Do not commit or push unless requested.
