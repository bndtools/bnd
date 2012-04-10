#!/bin/sh
exec nohup java -Dservice=%name% -Djar=%repoFile% -Dpid=$$ %vmArgs% -cp %path% aQute.jpm.service.ServiceMain %lock% %main% %args% 2>>/var/log/%service%.log >>/var/log/%name%.log &
