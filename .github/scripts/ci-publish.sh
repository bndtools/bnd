#!/usr/bin/env bash
set -ev
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :publish :gradle-plugins:publish "$@"
./mvnw -f old_pom.xml -Djdk.xml.maxGeneralEntitySizeLimit=500000 -Dmaven.repo.local=dist/m2  --batch-mode -Pdist -Dreleaserepo=file:dist/bundles deploy
