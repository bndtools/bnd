#!/usr/bin/env bash
set -ev
./gradlew --no-daemon --version
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :buildscriptDependencies :publish
# Temporarily ignore warnings like "Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0."
# so we use --warning-mode=all instead of --warning-mode=fail
#./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --warning-mode=fail :gradle-plugins:build
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --warning-mode=all :gradle-plugins:build
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :gradle-plugins:publish
