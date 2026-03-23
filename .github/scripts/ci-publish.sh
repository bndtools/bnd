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

# Detect release/RC build: if -snapshot: is not commented out in cnf/build.bnd,
# the build produces RELEASE versions (could also be RC1,2,3...).
# If it is commented out (e.g. #-snapshot) then it is a snapshot build.
IS_RELEASE=false
if grep -qE '^-snapshot:' cnf/build.bnd 2>/dev/null; then
	IS_RELEASE=true
fi

# 1. publish gradle-plugins to dist/bundles
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 ${GRADLE_GPG_ARGS} :gradle-plugins:publish "$@"

# 2. publish maven-plugins to dist/bundles (enforce dist profile, and explicitly disable jfrog)
./mvnw -Dmaven.repo.local=dist/m2 --batch-mode -Pdist,\!jfrog -Dreleaserepo=file:dist/bundles ${GPG_ARGS} deploy
# publish again for jfrog (with env.CANONICAL=true, where jfrog profile is chosen)
./mvnw -Dmaven.repo.local=dist/m2 --batch-mode -Pdist -Dreleaserepo=file:dist/bundles ${GPG_ARGS} deploy

# 3. publish bnd workspace to dist/bundles (and JFrog if CANONICAL)
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 :publish "$@"

# 4. Upload only releases / RCs to Sonatype Central Portal (separate from building)
if [[ "${IS_RELEASE}" == "true" ]]; then
	if [[ -n "${SONATYPE_BEARER:-}" ]]; then
		SONATYPE_OPTS=""
		if [[ -n "${SONATYPE_PUBLISHING_TYPE:-}" ]]; then
			SONATYPE_OPTS="--publishing-type ${SONATYPE_PUBLISHING_TYPE}"
		else 
			SONATYPE_OPTS="--publishing-type USER_MANAGED"
		fi
		
		./.github/scripts/sonatype-upload.sh ${SONATYPE_OPTS} dist/bundles
	else
		echo "Skipping Sonatype deployment due to missing SONATYPE_BEARER"
	fi
fi
