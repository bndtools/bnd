#!/bin/sh
exec java -Dpid=$$ -Xdock:name=%name% %jvmArgs% -cp "%classpath%" %main% "$@"
