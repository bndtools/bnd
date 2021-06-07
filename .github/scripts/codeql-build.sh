#!/usr/bin/env bash
unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS
./gradlew --no-daemon --version
./mvnw --version
./gradlew --no-daemon -Dmaven.repo.local=maven/target/m2 --continue testClasses :maven:test-compile "$@"
