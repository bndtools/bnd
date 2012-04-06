#!/bin/sh
exec java -Djar=%file% -Dpid=$$ -jar %file% $*
