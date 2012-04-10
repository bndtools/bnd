#!/bin/sh
exec nohup java -Dservice=%name% -Djar=%repoFile% -Dpid=$$ %jvmArgs% -cp %path% aQute.jpm.service.ServiceMain %lock% %main% %args% 2>>/var/log/%name%.log >>/var/log/%name%.log &
