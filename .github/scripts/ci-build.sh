#!/usr/bin/env bash
set -ev
./gradlew --no-daemon --version
./mvnw --version
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue :build :maven:deploy "$@"
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue :gradle-plugins:build
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue :gradle-plugins:publish
