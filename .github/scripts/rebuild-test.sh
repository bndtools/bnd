#!/usr/bin/env bash
set -ev
./gradlew --no-daemon --version
# Temporarily ignore warnings like "Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0."
# so we use --warning-mode=all instead of --warning-mode=fail
#./gradlew --no-daemon -Dmaven.repo.local=dist/m2 -Pbnd_snapshots=./dist/bundles --warning-mode=fail :buildscriptDependencies :build :publish "$@"
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 -Pbnd_snapshots=./dist/bundles --warning-mode=all :buildscriptDependencies :build :publish "$@"
