#!/usr/bin/env bash
set -ev
./gradlew --no-daemon --version
./mvnw --version
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue :build "$@"
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue :gradle-plugins:build
./mvnw -Dmaven.repo.local=dist/m2  -Dhttp.keepAlive=false -Dmaven.wagon.http.pool=false -Dmaven.wagon.httpconnectionManager.ttlSeconds=120  -Dhttp.connectionTimeout=100000 -Dhttp.retryCount=5 --batch-mode --no-transfer-progress install
