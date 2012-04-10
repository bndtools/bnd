#!/bin/sh
exec java -Djar=%repoFile% -Dpid=$$ -jar %repoFile% $*
