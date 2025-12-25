#!/usr/bin/env bash
set -ev
# Old way pre new Sonatype release
#./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :publish :gradle-plugins:publish "$@"

# publish gradle-plugins to dist/bundles
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :gradle-plugins:publish "$@"

# publish maven-plugins to dist/bundles
./mvnw -Dmaven.repo.local=dist/m2  --batch-mode -Pdist -Dreleaserepo=file:dist/bundles deploy

# Hack for sonatype release: Copy all above to cnf/cache/sonatype-release
# so that they get picked up by the workspace release process
mkdir -p cnf/cache/sonatype-release && \
cp -a \
  dist/bundles/biz/aQute/bnd/bnd* \
  dist/bundles/biz/aQute/bnd/biz* \
  cnf/cache/sonatype-release/

# Debugging: print cnf/cache/sonatype-release
ls cnf/cache/sonatype-release

# publish (release) bnd workspace, which signs and releases everything under cnf/cache/sonatype-release
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :publish "$@"