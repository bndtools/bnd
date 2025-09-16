#!/usr/bin/env bash
set -ev
./gradlew --no-daemon --version
./mvnw --version
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue :build "$@"
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue :gradle-plugins:build
./mvnw -f old_pom.xml -Djdk.xml.maxGeneralEntitySizeLimit=500000 -Dmaven.repo.local=dist/m2 --batch-mode --no-transfer-progress install
