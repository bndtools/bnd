#!/bin/sh
exec %java% %defines% -Dpid=$$ -DJPMREPO=%jpmRepoDir% %jvmArgs% -cp "%classpath%" %main% "$@"
