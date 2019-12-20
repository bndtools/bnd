#!/usr/bin/env bash
./gradlew --no-daemon --version
./mvnw --version
./gradlew --no-daemon -Dmaven.repo.local=maven/target/m2 --continue :build :maven:deploy
