#!/usr/bin/env bash
unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS
./gradlew --no-daemon --version
./gradlew --no-daemon -Dmaven.repo.local=maven/target/m2 :biz.aQute.bnd.gradle:build :biz.aQute.bnd.gradle:releaseNeeded
./gradlew --no-daemon -Dmaven.repo.local=maven/target/m2 -Pbnd_repourl=./dist/bundles :buildscriptDependencies :build "$@"
