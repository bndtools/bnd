___
___
# Launcher
bnd integrates an OSGi launcher. This launcher will start a framework and then install and start a list of bundles. The launching environment is described with a number of instructions that start with `-run`.

* `-runfw` — Specify the run framework in repository format
* `-runsystemcapabilities` — 
* `-runbundles` — A list of bundles to install in repository format
* `-runvm` — VM options. Use a comma to separate different options. E.g. `-Dx=4, -Dy=5`.
* `-runproperties` — System properties for the framework to create
* `-runpath` — A list of jars (not required to be bundles) that are put on the classpath and available from the system bundle. Any Export-Package headers are exported.
* `-runsystempackages` — An Export-Package list for packages exported by the system bundle.
