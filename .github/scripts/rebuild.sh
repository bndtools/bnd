#!/usr/bin/env bash
unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS
./gradlew --no-daemon --version
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :dist:releaseDependencies
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :gradle-plugins:build :gradle-plugins:publish
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 -Pbnd_repourl=./dist/bundles --warning-mode=fail :buildscriptDependencies :build "$@"
