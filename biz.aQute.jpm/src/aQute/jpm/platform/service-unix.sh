#!/bin/sh -e
exec nohup java -Dservice=%service% -Djar=%file% -Dpid=$$ %vmargs% -cp %path% %main% %args% 2>>/var/log/%service%.log >>/var/log/%service%.log &
