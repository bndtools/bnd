# Agents Guide for maven-plugins

This file defines agent workflows for work under `maven-plugins/`.

## Purpose

- Maintain the bnd Maven plugin suite, including:
  - `bnd-maven-plugin`
  - `bnd-indexer-maven-plugin`
  - `bnd-baseline-maven-plugin`
  - `bnd-export-maven-plugin`
  - `bnd-resolver-maven-plugin`
  - `bnd-testing-maven-plugin`
  - `bnd-run-maven-plugin`
  - `bnd-reporter-maven-plugin`
  - `bnd-generate-maven-plugin`

## Prerequisites

- Java 17+ for current repository branch expectations
- Maven 3.3.9+
- Use repository wrappers (`./gradlew`, `./mvnw`)
- Required build order:
  1. `./gradlew :build`
  2. `./mvnw install`

## Configure and Update

- Keep plugin versions and pluginRepository examples consistent when changing release channels:
  - snapshot repository: `https://bndtools.jfrog.io/bndtools/libs-snapshot/`
  - release/RC repository: `https://bndtools.jfrog.io/bndtools/libs-release/`
- Update plugin README snippets and integration tests when changing Mojo parameters or defaults.

## Build

- Build all Maven plugins:
  - `./mvnw install`
- Faster local cycle (skip invoker-heavy tests where appropriate):
  - `./mvnw install -Dinvoker.skip=true`

## Test

- Run module-specific tests with `-pl` and `-am` when iterating on one plugin.
- Re-run full plugin build before finalizing changes.

## Run and Validate

- Validate changed plugin behavior on a representative Maven sample project.
- For run/export/resolve/testing plugins, validate end-to-end goal execution, not just unit tests.
- Validate dist publication flow when needed:
  - `./mvnw -Pdist deploy`

## Cross-Repository Validation

- If plugin change depends on updated bnd core APIs, ensure the prerequisite workspace build is fresh (`./gradlew :build`).
- Run additional workspace checks if API surfaces shared with `biz.aQute.*` changed.
