#!/bin/sh
exec "java" -Dpid=$$ -DJPMREPO=%jpmRepoDir% %jvmArgs% -cp "%classpath%" %main% "$@"
