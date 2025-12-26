#!/usr/bin/env bash
set -ev
# Old way pre new Sonatype release
#./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :publish :gradle-plugins:publish "$@"

# Prepare GPG arguments for Maven and Gradle if environment variables are set
GPG_ARGS=""
GRADLE_GPG_ARGS=""
if [[ -n "${GPG_KEY_ID}" ]]; then
	GPG_ARGS="-Dgpg.keyname=${GPG_KEY_ID}"
	GRADLE_GPG_ARGS="-Psigning.gnupg.keyName=${GPG_KEY_ID}"
	if [[ -n "${GPG_PASSPHRASE}" ]]; then
		GRADLE_GPG_ARGS="${GRADLE_GPG_ARGS} -Psigning.gnupg.passphrase=${GPG_PASSPHRASE}"
	fi
fi

# publish gradle-plugins to dist/bundles
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 ${GRADLE_GPG_ARGS} :gradle-plugins:publish "$@"

# Publish twice (1st for dist/bundles, 2nd for jfrog)
# This double publishing is a hack to always populate dist/bundles,
# even if we deploy to jfrog

# publish maven-plugins to dist/bundles (enforce dist profile, and explicitly disable jfrog)
./mvnw -Dmaven.repo.local=dist/m2  --batch-mode -Pdist,\!jfrog -Dreleaserepo=file:dist/bundles ${GPG_ARGS} deploy

# publish again. This is for with env.CANONICAL=true, where jfrog profile is chosen
./mvnw -Dmaven.repo.local=dist/m2  --batch-mode -Pdist -Dreleaserepo=file:dist/bundles ${GPG_ARGS} deploy

pwd
ls -l
ls -lR dist

# Hack for sonatype release: Copy all above to cnf/cache/sonatype-release
# so that they get picked up by the workspace release process
mkdir -p cnf/cache/sonatype-release/biz/aQute/bnd/ && \
cp -a \
  dist/bundles/biz/aQute/bnd/* \
  cnf/cache/sonatype-release/biz/aQute/bnd/

# Debugging: print cnf/cache/sonatype-release
ls cnf/cache/sonatype-release

# publish (release) bnd workspace, which signs and releases everything under cnf/cache/sonatype-release
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :publish "$@"

# Debugging: print cnf/cache/sonatype-release
ls -lR cnf/cache/sonatype-release
