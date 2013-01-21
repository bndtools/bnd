#!/bin/sh
exec java -Dpid=$$ -Xdock:name="%title%" %jvmArgs% -cp "%classpath%" %main% "$@"
