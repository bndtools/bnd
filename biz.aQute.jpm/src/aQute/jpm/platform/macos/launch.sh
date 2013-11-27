#!/bin/sh
echo "
#################################################" 2>>%log% >>%log%

%prolog% 2>>%log% >>%log%

sudo -u %user% -i nohup %java% -Dservice=%name% -Dpid=$$ %jvmArgs% -cp %classpath% aQute.jpm.service.ServiceMain %lock% %main% %args% 2>>%log% >>%log% &
