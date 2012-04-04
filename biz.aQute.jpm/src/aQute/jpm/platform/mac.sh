#!/bin/sh
export JAR=%file%
exec java -jar %file% $*
