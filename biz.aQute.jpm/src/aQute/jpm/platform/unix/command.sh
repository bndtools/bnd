#!/bin/sh
exec java -Dpid=$$ %jvmArgs% -cp "%classpath%" %main% "$@"
