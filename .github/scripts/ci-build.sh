#!/usr/bin/env bash
set -ev

# gpg key handling for github action build
if [[ -n "${GPG_PRIVATE_KEY}" && -n "${GPG_PASSPHRASE}" ]]; then
    echo "GPG environment variables validated successfully"
    echo "${GPG_PRIVATE_KEY}" | \
    gpg --batch \
        --yes \
        --pinentry-mode loopback \
        --passphrase "${GPG_PASSPHRASE}" \
        --import

    echo -e "#\n# list secret keys\n#\n"
    gpg --list-secret-keys --keyid-format LONG
    export GPG_TTY=$(tty)
fi

# build
./gradlew --no-daemon --version
./mvnw --version
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue :build "$@"
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue :gradle-plugins:build
./mvnw -f old_pom.xml -Djdk.xml.maxGeneralEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0 -Djdk.xml.entityExpansionLimit=0 -Dmaven.repo.local=dist/m2 --batch-mode --no-transfer-progress install
