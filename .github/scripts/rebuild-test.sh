#!/usr/bin/env bash
set -ev
./gradlew --no-daemon --version
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 -Pbnd_repourl=./dist/bundles --warning-mode=fail :buildscriptDependencies :build "$@"
