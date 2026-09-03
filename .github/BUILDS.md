# CI/CD Workflows Overview

This document describes the purpose of each GitHub Actions workflow in `.github/workflows/`, the scripts they invoke from `.github/scripts/`, and suggests performance improvements.

## Workflow Inventory

| Workflow file | Name | Trigger | Purpose |
|---|---|---|---|
| [cibuild.yml](workflows/cibuild.yml) | CI Build | push (non-dependabot), pull_request | Main build/test/publish pipeline. Builds bnd, bndtools, gradle-plugins and maven-plugins; publishes snapshots on `master`/`next`; uploads a PR-scoped p2 repo for manual testing. |
| [rebuild.yml](workflows/rebuild.yml) | Rebuild | push, pull_request | Verifies that projects can be rebuilt purely from already-published `dist/bundles` artifacts (no network deps), across JDK 17/21/25. Simulates "downstream consumer" builds against the just-built bnd. |
| [codeql.yml](workflows/codeql.yml) | CodeQL | push/PR touching `**.java` | Static security analysis (CodeQL) of the Java codebase. |
| [docs.yml](workflows/docs.yml) | Docs Build | push/PR touching `docs/**`, manual | Builds the Jekyll documentation site and deploys it to GitHub Pages (deploy only on `master`). |
| [docs_generate.yml](workflows/docs_generate.yml) | Generate Docs | after CI Build succeeds on `master`, push to `docs/**/_ext/**`, manual | Regenerates command/macro reference docs from the freshly built `biz.aQute.bnd` jar and opens a PR with any diffs. |
| [gradle_plugin_release.yml](workflows/gradle_plugin_release.yml) | wip | manual | Debug/utility workflow to publish the Gradle plugin to the Gradle Plugin Portal from the `next` branch. |
| [postrelease.yml](workflows/postrelease.yml) | post release | manual (after a release is on Maven Central) | Generates static release docs, creates a new `bndtools/workspace` template branch, and opens a PR against `master` to finalize baselines/docs for the release. |
| [cleanup_jfrog_p2_repos.yml](workflows/cleanup_jfrog_p2_repos.yml) | Cleanup JFrog p2 Repos | PR close, nightly cron, manual | Deletes stale per-PR/per-branch p2 repositories from JFrog that no longer correspond to an open PR or existing branch. |
| [stale.yml](workflows/stale.yml) | Stale | nightly cron | Marks/closes inactive issues and PRs. |
| [wrapper.yml](workflows/wrapper.yml) | Wrapper | push/PR touching the Gradle wrapper | Validates the Gradle wrapper JAR checksum against the official registry (supply-chain check). |
| [cache-reset.yml](workflows/cache-reset.yml) | Cache Reset | manual | Invalidates the `dist/m2` and `cnf/cache` build caches (see [Caching](#caching) below) and optionally kicks off a fresh CI Build run against empty caches. |

## CI Build (`cibuild.yml`) Matrix Detail

- **`build-canonical`** — single leg (`ubuntu-latest`, JDK 17), the "source of truth" build.
  - Runs [ci-build.sh](scripts/ci-build.sh): `gradlew build` (bnd workspace) → `gradlew :gradle-plugins:build` → `mvnw install` (maven-plugins), sequentially.
  - On push to `master`/`next` (not PRs), runs [ci-publish.sh](scripts/ci-publish.sh) to publish gradle-plugins, maven-plugins and the bnd workspace to JFrog, and — for release/RC builds only — to Sonatype Central.
  - Also produces and uploads/publishes the `org.bndtools.p2` repo (as a build artifact for PRs, or to JFrog for pushes/same-repo PRs).
- **`build-others`** — depends on (`needs:`) `build-canonical` and only runs if it didn't fail/cancel. Matrix: `{ubuntu-latest, windows-latest} × {java 21, 25}` plus an extra include for `windows-latest`/JDK 17 (JDK 17 on ubuntu is excluded since it's already covered by `build-canonical`). Runs the same [ci-build.sh](scripts/ci-build.sh) without publishing.
  - `fail-fast` is only relaxed (`false`) for pushes to `master`/`next` in the canonical repo; PRs and forks fail fast.

## Rebuild (`rebuild.yml`) Detail

- **`build`** job: one leg (ubuntu/JDK17). Runs [rebuild-build.sh](scripts/rebuild-build.sh), which does `:buildscriptDependencies :publish` then `:gradle-plugins:build`/`publish`, producing `dist/bundles`, uploaded as an artifact.
- **`rebuild`** job: downloads `dist/bundles` and, across JDK 17/21/25, runs [rebuild-test.sh](scripts/rebuild-test.sh) (`testClasses`, `:dist:jarDependencies`, gradle-plugins `testClasses`, and `mvnw test-compile`) — proving downstream projects compile/test against the published artifacts on each supported JDK without rebuilding bnd itself.

## Other scripts

- [codeql-build.sh](scripts/codeql-build.sh) — `:buildscriptDependencies :build :publish`, used only to produce compiled classes for CodeQL to scan.
- [docs.sh](scripts/docs.sh) — Jekyll build for the doc site.
- [release.sh](scripts/release.sh) / [sonatype-upload.sh](scripts/sonatype-upload.sh) — release-time signing/upload helpers invoked from `ci-publish.sh`/manual release flows.

## Caching

### Does the bnd configuration use `dist/m2`?

Yes. `-Dmaven.repo.local=dist/m2` is passed to every `gradlew`/`mvnw` invocation in the CI scripts. That system property is read directly by bnd's Maven-backed repository code (`MavenRepository`, `Maven`, `BndPomRepository`, `MavenBndRepository` — see `MAVEN_REPO_LOCAL = System.getProperty("maven.repo.local", "~/.m2/repository")`), so it is *not* just Maven's (`mvnw`) local repo — it is also the local Aether repository backing every `MavenBndRepository` plugin declared in [cnf/ext/repositories.bnd](../cnf/ext/repositories.bnd): `Main` (Maven Central + Sonatype snapshots), `Local`, `Release` (points at `dist/bundles`), `JFrog`, and `Baseline`. In other words, `dist/m2` is the single shared download/artifact cache for the entire bnd workspace build, the gradle-plugins build, and the maven-plugins (`mvnw`) build alike.

### Other cache folders

| Folder | Populated by | Currently cached between CI runs? |
|---|---|---|
| `dist/m2` | Aether/Maven resolution for all `MavenBndRepository` plugins + `mvnw` | No (new checkout every run) |
| `~/.gradle` (wrapper, dependency cache, and — if enabled — the build cache) | `gradle/actions/setup-gradle` | Wrapper/dependency cache: yes, automatically. Task-output build cache: not enabled (see suggestion #1). |
| `cnf/cache/p2-<name>` | `aQute.bnd.repository.p2.provider.P2Repository` (the `ECFRSSDK` plugin, and the commented-out Eclipse platform repo) — downloaded p2 index/content | No |
| `cnf/cache/pom-<name>.xml` | `BndPomRepository` (not used by the root workspace config, only test fixtures) | N/A for root build |
| `docs/vendor/bundle` | Ruby Bundler, used by the Jekyll doc site | Yes, via `ruby/setup-ruby`'s `bundler-cache: true` in [docs.yml](workflows/docs.yml) |

`dist/m2` and `cnf/cache` are the two folders worth caching explicitly for the root workspace build; the rest are either already cached (Gradle/Ruby) or unused by the root config (pom-repo/Sonatype dirs are only exercised by test-fixture workspaces).

### Caching solution + manual reset

[cibuild.yml](workflows/cibuild.yml), [rebuild.yml](workflows/rebuild.yml), and [docs_generate.yml](workflows/docs_generate.yml) now each carry `actions/cache` steps for `dist/m2` and `cnf/cache`, keyed as:

```
dist-m2-<CACHE_EPOCH>-<runner.os>-<hash of pom.xml/*.bnd/*.gradle*>
cnf-cache-<CACHE_EPOCH>-<hash of cnf/ext/repositories.bnd>
```

`CACHE_EPOCH` is a repository variable (`vars.CACHE_EPOCH`, defaulting to `1` if unset) baked into every cache key. Bumping it invalidates *every* cache key at once without needing to enumerate or delete anything — old entries simply become unreachable and GitHub garbage-collects them under its normal 7-day/10 GB cache eviction rules.

The new [cache-reset.yml](workflows/cache-reset.yml) workflow provides this as an on-demand action:

1. Trigger it manually (Actions tab → "Cache Reset" → Run workflow), typing `RESET` to confirm.
2. It bumps the `CACHE_EPOCH` repository variable (via `gh variable set`), which invalidates the `dist-m2-*`/`cnf-cache-*` keys used by all three workflows above.
3. It also proactively deletes existing Actions caches via `gh cache delete` for immediate cleanup (best-effort; the epoch bump alone is sufficient to force cold caches on the next run even if deletion is skipped/fails).
4. If `trigger_build` is left at its default (`true`), it dispatches [cibuild.yml](workflows/cibuild.yml) on the given `ref`, giving a full build with empty caches without waiting for the next push/PR. (`cibuild.yml` was given a `workflow_dispatch` trigger to support this.)

## Performance Improvement Suggestions

1. **Enable the Gradle build cache.** Neither `gradle.properties` nor the workflows set `org.gradle.caching=true`, and no remote/HTTP build cache is configured (only Gradle Enterprise build *scans* are published, not cache nodes). Turning on the local build cache plus `gradle/actions/setup-gradle`'s cache-write on `master`/`next` (read-only elsewhere) would let `build-others`, `rebuild`'s JDK 21/25 legs, and `codeql-build.sh` reuse task outputs from `build-canonical` instead of recompiling everything from scratch on every JDK/OS leg.
2. **Decouple `build-others` from `build-canonical`.** `needs: [build-canonical]` fully serializes the matrix — the 4–5 "others" legs don't start until the canonical build finishes, even though they don't consume its outputs. Running them in parallel (perhaps gating only the *publish* step, not the whole job, on canonical success) would cut wall-clock CI time roughly in half.
3. **Parallelize the three build systems within `ci-build.sh`.** Gradle workspace build → gradle-plugins build → Maven install run strictly sequentially in one job. Maven-plugins and gradle-plugins builds don't depend on each other and could run concurrently (`&`/`wait`) or be split into separate matrix/job entries that fan out and only join for the publish step.
4. ~~**Cache the Maven local repository.**~~ Implemented — see [Caching](#caching): `dist/m2` (and `cnf/cache`) are now cached with `actions/cache` in `cibuild.yml`, `rebuild.yml` and `docs_generate.yml`, invalidated as a group via the `CACHE_EPOCH` repository variable and the manual [cache-reset.yml](workflows/cache-reset.yml) workflow.
5. **Trim the PR matrix.** Every PR currently builds 6 OS/JDK combinations (1 canonical + 5 others), including `windows-latest` which is billed at 2× the Linux rate and is typically the slowest runner. Consider running the full matrix only on pushes to `master`/`next` (where `fail-fast: false` already signals "we want full coverage") and a reduced subset (e.g. canonical + one Windows leg) on PRs, expanding to the full matrix only via label or on merge queue.
6. **Skip `:publish`/`test`/full `:build` in `codeql-build.sh`.** CodeQL only needs compiled bytecode for the extractor; running `:build` (which includes tests) and `:publish` adds unrelated time. Using something closer to `compileJava`/`assemble` across affected projects would speed up the CodeQL job without weakening analysis.
7. ~~**Add setup-gradle caching to `docs_generate.yml`.**~~ Implemented — `gradle/actions/setup-gradle` plus a `dist/m2` cache step were added alongside the existing Java setup.
8. **Reuse `rebuild.yml`'s compiled output across its JDK matrix.** The `rebuild` job recompiles (`testClasses`) independently for JDK 17/21/25; with build cache enabled (#1) plus a shared cache key across those legs, only the JDK-specific bytecode/tests that actually differ would need recompilation.
