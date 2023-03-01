#!/usr/bin/env bash
set -ev
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :publish "$@"
