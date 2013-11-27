#!/bin/sh
exec %java% %defines% -Dpid=$$ -DJPMREPO=%jpmRepoDir% -Xdock:name="%title%" %jvmArgs% -cp "%classpath%" %main% "$@"
