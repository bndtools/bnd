#!/usr/bin/env bash
set -ev
./gradlew --no-daemon --version
./mvnw --version
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue testClasses :dist:jarDependencies :maven:test-compile "$@"
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue :gradle-plugins:testClasses
