# Agents Guide for gradle-plugins

This file defines agent workflows for work under `gradle-plugins/`.

## Purpose

- Maintain and evolve bnd Gradle plugins used for:
  - Non-Bnd workspace Gradle builds (`biz.aQute.bnd.builder` and task types)
  - Bnd workspace Gradle builds

## Prerequisites

- Java 17+
- Gradle 8+
- Use wrapper from repository root (`./gradlew`)
- Build core workspace artifacts before plugin builds:
  - `./gradlew :build`

## Configure and Update

- Version/config sources to keep aligned:
  - `gradle-plugins/gradle.properties`
  - plugin module `build.gradle(.kts)` files
  - examples/snippets in docs and README text
- When changing plugin IDs, tasks, or DSL behavior, update docs and regression coverage together.

## Build

- Primary build:
  - `./gradlew :gradle-plugins:build`
- Faster local cycle (skip tests):
  - `./gradlew :gradle-plugins:build -x test`

## Test

- Run full checks for plugin tree:
  - `./gradlew :gradle-plugins:check`
- For focused iteration, run affected module tests/tasks only.

## Run and Validate

- Validate plugin behavior in a sample consumer project using:
  - `biz.aQute.bnd.builder` plugin application
  - key tasks (`Bundle`, `Baseline`, `Resolve`, `Export`, `TestOSGi`, `Index`, `Bndrun`)
- Validate with configuration-cache-sensitive scenarios when changing task property evaluation behavior.
- Validate publish wiring when needed:
  - `./gradlew :gradle-plugins:publish`

## Release/Compatibility Validation

- Confirm compatibility assumptions documented in README (Java/Gradle minimums).
- Confirm plugin marker artifact/version references are consistent with repository release line.
- Run workspace build if cross-module APIs changed:
  - `./gradlew :build`
