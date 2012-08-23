#!/bin/sh
exec nohup java -Dservice=%name% -Dpid=$$ %jvmArgs% -cp %classpath% aQute.jpm.service.ServiceMain %lock% %main% %args% 2>>/var/log/%name%.log >>/var/log/%name%.log &
