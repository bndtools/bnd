#!/usr/bin/env bash
set -ev
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :publish :gradle-plugins:publish "$@"
./mvnw -Dmaven.repo.local=dist/m2  --batch-mode -Pdist -Dreleaserepo=file:dist/bundles deploy
