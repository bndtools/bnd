#!/usr/bin/env bash
set -ev
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :publish :gradle-plugins:publish "$@"
./mvnw -Dmaven.repo.local=dist/m2 -Dhttp.keepAlive=false -Dmaven.wagon.http.pool=false -Dmaven.wagon.httpconnectionManager.ttlSeconds=120  -Dhttp.connectionTimeout=100000 -Dhttp.retryCount=5  --batch-mode -Pdist -Dreleaserepo=file:dist/bundles deploy
