@echo off
del launch.properties.tmp >nul 2>&1
copy launch.properties launch.properties.tmp >nul 2>&1
java -cp ../../../../../../bndtools/bndtools.repository.base/templates/cnfs/standard/buildrepo/org.apache.felix.framework/org.apache.felix.framework-4.0.1.jar;../../../../../biz.aQute.launcher/generated/biz.aQute.launcher.jar -Dlauncher.properties=launch.properties.tmp aQute.launcher.Launcher 
