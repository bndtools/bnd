#!/bin/bash
rm launch.properties.tmp  > /dev/null 2>&1
cp launch.properties launch.properties.tmp
java -cp ../../../../bndtools/bndtools.repository.base/templates/cnfs/default/buildrepo/org.apache.felix.framework/org.apache.felix.framework-4.0.1.jar:../../../biz.aQute.launcher/generated/biz.aQute.launcher.jar -Dlauncher.properties=launch.properties.tmp aQute.launcher.Launcher 
