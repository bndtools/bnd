# Agents Guide for bnd Repository

This file complements .github/copilot-instructions.md and defines practical workflows for AI agents working in this repository.

## Scope and Goal

- Use this guide to orchestrate tasks across core libraries, CLI, Eclipse tooling, Gradle plugins, Maven plugins, and docs.
- Prefer small, verifiable changes with targeted module builds/tests before full-repo validation.

## Prerequisites

- OS: Windows, macOS, or Linux.
- Shell: Use a POSIX-like shell when possible (`bash` on Windows via Git Bash/MSYS2).
- Java: 17+ required for the `master` branch.
- For docs work, prefer Docker-based Jekyll workflows over local Ruby installation.
- Build tools:
  - Gradle wrapper: `./gradlew` or `gradlew.bat`
  - Maven wrapper: `./mvnw` or `mvnw.cmd`

## Workflow for docs/

### Configure and Update

- Check Docker availability before docs build/run commands:
  - `docker --version`
  - `docker compose version` (if compose workflows are used)
- Use local Ruby/Bundler setup only as fallback when Docker is unavailable.

### Build and Run

- Preferred local docs server (Docker):
  - `docker run --rm -it -p 4000:4000 -v "$PWD":/srv/jekyll -w /srv/jekyll jekyll/jekyll:4 jekyll serve -w --incremental --host 0.0.0.0`
- Preferred docs build (Docker):
  - `docker run --rm -it -v "$PWD":/srv/jekyll -w /srv/jekyll jekyll/jekyll:4 jekyll build`
- Fallback scripts:
  - `cd docs && ./run.sh`
  - `cd docs && ./build.sh`

### Validate

- Verify nav, search, static links, and release pages in the generated site.
- Keep top-level docs policy aligned with `docs/Agents.md`.

## Repository Purpose and Namespaces

- `biz.aQute.bndlib` and related `biz.aQute.*` modules:
  - Core OSGi analysis/building capabilities, resolver/repository support, testing/runtime utilities.
- `biz.aQute.bnd`:
  - bnd CLI entrypoint and command set for building, inspecting, resolving, exporting, and generating docs.
- `bndtools.*` and `org.bndtools.*`:
  - Eclipse/Bndtools IDE integrations, launch/debug/build UX, APIs, and supporting bundles.

## Workflow for biz.aQute.*

### Configure and Update

- Keep Java 17 active in shell before builds.
- Update dependency/version inputs in:
  - root `gradle.properties`
  - module `bnd.bnd`
  - module `build.gradle` when present
- For release/version work, align with repository release scripts and docs.

### Build

- Fast local compile for changed module:
  - `./gradlew :biz.aQute.bndlib:build -x test`
- Recommended baseline for workspace projects:
  - `./gradlew :build`

### Test

- Targeted test class:
  - `./gradlew :biz.aQute.bndall.tests:test --tests "fully.qualified.TestClass"`
- Single test method:
  - `./gradlew :biz.aQute.bndall.tests:test --tests "fully.qualified.TestClass.testMethod"`

### Run

- Build CLI jar first via workspace build, then run commands from generated jar, for example:
  - `java -jar biz.aQute.bnd/generated/biz.aQute.bnd.jar version`

### Validate

- Validate behavior with targeted tests first, then module-level `build`, then `:build` when scope is broad.
- For API/package changes, ensure semantic versioning and baselining expectations are preserved.

## Workflow for bndtools.* and org.bndtools.*

### Configure and Update

- Treat these as Eclipse plugin bundles (manifest-first, OSGi aware).
- Keep plugin metadata aligned when adding extension points/services.

### Build

- Build a specific module quickly:
  - `./gradlew :bndtools.core:build -x test`
- Build all workspace projects (includes bndtools modules):
  - `./gradlew :build`

### Test

- Run module tests:
  - `./gradlew :bndtools.core.test:test`
- Prefer focused regression tests for marker handling, launchers, facade usage, and workspace model behavior.

### Run

- For IDE behavior validation, launch Bndtools through the platform-specific `.bndrun` launchers under `bndtools.core`.

### Validate

- Confirm expected markers, launch configs, and extension contributions in a running Eclipse instance.
- Verify no regressions in existing launch/build flows and existing test suites.

## End-to-End Validation Ladder

- Step 1: Build/test only changed modules.
- Step 2: Build corresponding plugin set (`:gradle-plugins:build` or `./mvnw install`) if impacted.
- Step 3: Run `./gradlew :build` for workspace-wide confidence.
- Step 4: For publish path checks, run:
  - `./gradlew :publish`
  - `./gradlew :gradle-plugins:publish`
  - `./mvnw -Pdist deploy`

## Agent Operating Rules

- Prefer wrappers (`./gradlew`, `./mvnw`) over system-installed tools.
- Prefer targeted tests first, then broaden scope.
- Avoid broad formatting-only changes.
- Keep changes backward compatible unless explicitly scoped for breaking change work.
