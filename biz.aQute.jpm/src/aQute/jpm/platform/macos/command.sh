#!/bin/sh
exec java %defines% -Dpid=$$ -Xdock:name="%title%" %jvmArgs% -cp "%classpath%" %main% "$@"
