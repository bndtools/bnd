#!/bin/sh
exec java -Djar=%repoFile% -Dpid=$$ %jvmArgs% -jar %repoFile% "$@"
