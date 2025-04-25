#!/usr/bin/env bash
set -ev
./gradlew --no-daemon --version

# JDK24: ignore warnings like "Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0."
# so we use --warning-mode=all instead of --warning-mode=fail
# < JDK24 use fail

# Detect Java major version
JAVA_VERSION=$(java -version 2>&1 | awk -F[\".] '/version/ {print $2}')
echo "Detected Java version: $JAVA_VERSION"

# Choose warning mode based on Java version
if [ "$JAVA_VERSION" -ge 24 ]; then
  WARNING_MODE="all"
else
  WARNING_MODE="fail"
fi

./gradlew --no-daemon -Dmaven.repo.local=dist/m2 -Pbnd_snapshots=./dist/bundles --warning-mode=$WARNING_MODE :buildscriptDependencies :build :publish "$@"
