#!/usr/bin/env bash
set -ev

# Prepare GPG arguments for Maven and Gradle if environment variables are set
GPG_ARGS=""
GRADLE_GPG_ARGS=""
if [[ -n "${GPG_KEY_ID:-}" ]]; then
	GPG_ARGS="-Dgpg.keyname=${GPG_KEY_ID}"
	GRADLE_GPG_ARGS="-Psigning.gnupg.keyName=${GPG_KEY_ID}"
	if [[ -n "${GPG_PASSPHRASE:-}" ]]; then
		GRADLE_GPG_ARGS="${GRADLE_GPG_ARGS} -Psigning.gnupg.passphrase=${GPG_PASSPHRASE}"
	fi
fi

# Detect snapshot build: if #-snapshot is commented out in cnf/build.bnd,
# the build produces SNAPSHOT versions
IS_SNAPSHOT=false
if grep -qE '^#-snapshot:' cnf/build.bnd 2>/dev/null; then
	IS_SNAPSHOT=true
fi

# 1. publish gradle-plugins to dist/bundles
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 ${GRADLE_GPG_ARGS} :gradle-plugins:publish "$@"

# 2. publish maven-plugins to dist/bundles (enforce dist profile, and explicitly disable jfrog)
./mvnw -Dmaven.repo.local=dist/m2 --batch-mode -Pdist,\!jfrog -Dreleaserepo=file:dist/bundles ${GPG_ARGS} deploy
# publish again for jfrog (with env.CANONICAL=true, where jfrog profile is chosen)
./mvnw -Dmaven.repo.local=dist/m2 --batch-mode -Pdist -Dreleaserepo=file:dist/bundles ${GPG_ARGS} deploy

# 3. publish bnd workspace to dist/bundles (and JFrog if CANONICAL)
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :publish "$@"

# 4. Upload to Sonatype Central Portal (separate from building)
if [[ -n "${SONATYPE_BEARER:-}" ]]; then
	SONATYPE_OPTS=""
	if [[ -n "${SONATYPE_PUBLISHING_TYPE:-}" ]]; then
		SONATYPE_OPTS="--publishing-type ${SONATYPE_PUBLISHING_TYPE}"
	fi
	if [[ "${IS_SNAPSHOT}" == "true" ]]; then
		SONATYPE_OPTS="${SONATYPE_OPTS} --snapshot"
	fi
	./.github/scripts/sonatype-upload.sh ${SONATYPE_OPTS} dist/bundles
fi
