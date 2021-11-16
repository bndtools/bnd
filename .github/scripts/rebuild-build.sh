#!/usr/bin/env bash
set -ev
./gradlew --no-daemon --version
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :publish
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --warning-mode=fail :gradle-plugins:build
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :gradle-plugins:publish
