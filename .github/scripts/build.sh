#!/usr/bin/env bash
set -ev
unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS
./gradlew --no-daemon --version
./mvnw --version
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue :build :maven:deploy "$@"
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue :gradle-plugins:build :gradle-plugins:publish
