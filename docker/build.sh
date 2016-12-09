#!/bin/bash
set -e

# Edit these properties when moving to a new version.
VERSION=3.3.0
# SHA-256...
SHA=b6b68dfcd0f5ba767a202bf35eb3eb964c63e679e8217dd514dac807e6cedee8

mkdir -p tmp
if [ ! -f tmp/bnd.jar ]; then
	echo Downloading bnd JAR version $VERSION
	curl https://search.maven.org/remotecontent?filepath=biz/aQute/bnd/biz.aQute.bnd/$VERSION/biz.aQute.bnd-$VERSION.jar > tmp/bnd.jar
else
	echo Already downloaded bnd JAR version $VERSION. If SHA check fails, try deleting tmp folder.
fi

echo Checking SHA...
echo "$SHA  tmp/bnd.jar" > tmp/checksum
shasum -a 256 -c tmp/checksum

echo Building Docker image
docker build --quiet -t bndtoolsorg/bnd:$VERSION -t bndtoolsorg/bnd:latest .

echo DONE! Try the following:
echo alias bnd-docker=\''docker run -it -v $HOME:$HOME -v $(pwd):/data' bndtoolsorg/bnd:latest\'
echo bnd-docker version
