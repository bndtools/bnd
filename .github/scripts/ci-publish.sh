#!/usr/bin/env bash
set -ev
# Old way pre new Sonatype release
#./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :publish :gradle-plugins:publish "$@"

# publish gradle-plugins to dist/bundles
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :gradle-plugins:publish "$@"

# Publish twice (1st for dist/bundles, 2nd for jfrog)
# This double publishing is a hack to always populate dist/bundles,
# even if we deploy to jfrog

# publish maven-plugins to dist/bundles (enforce dist profile, and explicitly disable jfrog)
./mvnw -Dmaven.repo.local=dist/m2  --batch-mode -Pdist,\!jfrog -Dreleaserepo=file:dist/bundles deploy

# publish again. This is for with env.CANONICAL=true, where jfrog profile is chosen
./mvnw -Dmaven.repo.local=dist/m2  --batch-mode -Pdist -Dreleaserepo=file:dist/bundles deploy

pwd
ls -l
ls -lR dist

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

# Debugging: print cnf/cache/sonatype-release
ls cnf/cache/sonatype-release
