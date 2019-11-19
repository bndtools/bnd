---
order: 300
title: Launching
layout: default
---

bnd integrates an OSGi launcher. This launcher will start a framework and then install and start a list of bundles. Launch descriptions are defined in a _bndrun_ file. (A bnd.bnd file can actually also act as a bndrun file.) The bndrun file inherits properties from the workspace, not the profile.

The launching environment is described with a number of instructions that start with `-run`.

* `-runfw` — Specify the run framework in repository format
* `-runsystemcapabilities` — Capabilities added to the environment
* `-runbundles` — A list of bundles to install in repository format
* `-runvm` — VM options. Use a comma to separate different options. E.g. `-Dx=4, -Dy=5`.
* `-runproperties` — System properties for the framework to create
* `-runpath` — A list of jars (not required to be bundles) that are put on the classpath and available from the system bundle. Any Export-Package and Provide-Capabilityheaders  are provided as packages/capabilities from the framework. The `-runpath` can also override the default launcher.
* `-runsystempackages` — An Export-Package list for packages exported by the system bundle.
* `-runkeep` – Keep the framework working directory. That is, do not clean at start up
* `-runstorage` – The working directory
* `-runnoreferences` – Do not use the `reference:` scheme when installing. (Sometimes required on Windows).


Additional properties can be specified, and can be inherited from the workspace, that are specific for a launcher or are used for exporting a bndrun to an executable format like OSGi Subsystems, KARs, WARs, or executable JARs.
 
## Launcher Architecture

Launchers are not build into bnd, the actual launching strategy is parameterized. A launcher is associated with a bnd or bndrun file by placing a JAR on the `-runpath`. A JAR should have a `Launcher-Plugin` header to be a launcher. If no launcher is found on the `-runpath` then the built-in `biz.aQute.launcher` will be used. 

The plugin maps the bnd model specified in a bndrun or bnd file to an external execution. In general this plugin launches or contacts a VM, installs the `-runpath`, installs `-runbundles`, fires up the bundles. The plugin is also called when the bundles or settings have changed so that it can dynamically update the bundles on the running VM.

There are currently two launchers in the bnd repository:

* `biz.aQute.launcher` – Default launcher. This launcher starts a VM on the local machine and keeps it synchronized with the changes in the IDE. The launcher can also create an executable JAR.
* `biz.aQute.remote.launcher` – A launcher that can communicate with a remote VM, optionally installs a `-runpath` (among which a framework), and then synchronizes the `-runbundles` and a number of other properties with the remote VM.

## biz.aQute.launcher

The default launcher in bnd. It creates a new VM with the given options, creates a framework using the OSGi launching API, and then manages the bundles on this framework with the OSGi launching API. It can update the remote framework in real time by changing a properties file that is watched for by the launcher class running in the remote framework. 

### Example bndrun file

	-runfw:                     org.apache.felix.framework;version='[4,5)'
	-runbundles: \
		org.apache.felix.shell,\
		org.apache.felix.shell.tui,\
		org.apache.felix.scr,\
		org.apache.felix.http.jetty,\
		org.apache.felix.configadmin,\
		org.apache.felix.metatype,\
		org.apache.felix.log,\
		org.apache.felix.webconsole,\
		osgi.cmpn,\
		aQute.xray.badbundle;version=latest,\
		aQute.xray.plugin;version=latest,\
		aQute.xray.hello;version=latest,\
		com.springsource.org.apache.commons.fileupload;version=1.2.1,\
		com.springsource.org.apache.commons.io;version=1.4.0,\
		com.springsource.org.json;version=1.0.0
	
	-runproperties:            org.osgi.service.http.port=8080
	
	-runrequires:\
		bundle:(symbolicname=org.apache.felix.shell),\
		bundle:(symbolicname=org.apache.felix.shell.tui),\
		bundle:(symbolicname=org.apache.felix.webconsole),\
		bundle:(symbolicname=org.apache.felix.configadmin),\
		bundle:(symbolicname=org.apache.felix.metatype),\
		bundle:(symbolicname=org.apache.felix.log),\
		bundle:(&(symbolicname=osgi.cmpn)(version>=4.2)),\
		bundle:(&(symbolicname=org.apache.felix.scr)(version>=1.6.0))

### Exports from the Runpath

The launcher analyzes the `-runpath` JARs. Any additional capabilities in the manifest (packages and Provide Capability headers) in these JARs are automatically added to the framework.

### Runtime Information

The launcher registers a service with object class `Object` that provides some runtime information. The following properties are set on this service:

* `launcher.arguments` – The arguments passed to the `main` method.
* `launcher.properties` – The properties handed to the launcher. These properties are modified by any overriding properties that were set from the command line.
* `launcher.ready` – A boolean set to true, indicating the launcher has done all its work.
* `service.ranking` – Set to -1000

## Embedded Activators

The launcher supports _embedded activators_. These are like normal Bundle Actviator classes but are instead found on the `-runpath`. Any bundle that has the header `Embedded-Activator` will be started. The start can happen at 3 points that are identified with a static field in the Embedded Activator class. This field is called `IMMEDIATE`. For example:

    public class MyEmbeddedActivator implements BundleActivator {
        public static String IMMEDIATE = "AFTER_FRAMEWORK_INIT";
        ...
    } 

The `IMMEDIATE` field can have the following values:

* `"AFTER_FRAMEWORK_INIT"` –  The Embedded Activator is called after the Framework is initialized but before the framework is started. This means that no bundles are started yet.
*  `"BEFORE_BUNDLES_START"` – The Embedded Activator is called after the framework has been started but before the bundles are explicitly started _by the launcher_. This will always happening in start level 1. If the framework was started from an existing configuration then any bundles in start level 1 that were persistently started will therefore have been started before the Embedded Activator is started. The launcher starts bundles persistently so if the same configuration is restarted they will be started after the framework is started.
* `"AFTER_BUNDLES_START"` – Will start the Embedded Activators _after_ all bundles have been persistently started. Since this happens at start level 1, some bundles in higher start levels will not be active.

The reason strings are used is to not require the need for extraneous types on the executable's class path. If a string in `IMMEDIATE` is used that is not part of the previous one then a message must be logged. The behavior will then be `"AFTER_BUNDLES_START"`. Other strings are reserved for future extensions.

For example: 

    public class MyActivator implements BundleActivator {
        public static String IMMEDIATE = "BEFORE_BUNDLES_START";

        public void start(BundleContext context) {}
        
        public void stop(BundleContext context) {}
    }

    bnd.bnd:
        Embedded-Activator: com.example.MyActivator

For backward compatibility reason the `IMMEDIATE` field the launcher will also recognize a `boolean` field. 

* `true` – Corresponds to the String `BEFORE_BUNDLES_START`. 
* `false` – Will be the `"AFTER_BUNDLES_START"` case

It is recommended to update to one of the strings instead of the boolean and not use this pattern in new setups.

## Startlevels

The `-runbundles` instruction supports an `startlevel` attribute. If one or more of the bundles listed in the `-runbundles` instruction
uses the `startlevel` attribute then the launcher will assign a startlevel to each bundle. This is currently supported for the
normal launcher and not for Launchpad nor the remote launcher.

If a bundle has a `startlevel` attribute then this must be an integer > 0, otherwise it is ignored. Bundles that have no
`startlevel` attribute will be assign the maximum assigned startlevel attribute + 1. For example, given the following 
bundles:

	-runbundles: \
		org.apache.felix.configadmin;version='[1.8.8,1.8.9)',\
		org.apache.felix.http.jetty;version='[3.2.0,3.2.1)';startlevel=10,\
		org.apache.felix.http.servlet-api;version='[1.1.2,1.1.3)',\
		...
		osgi.enroute.twitter.bootstrap.webresource;version='[3.3.5,3.3.6)';startlevel=12,\
		osgi.enroute.web.simple.provider;version='[2.1.0,2.1.1)'

The `org.apache.felix.configadmin`,  `org.apache.felix.http.servlet-api`, and `osgi.enroute.web.simple.provider` do not specify 
a `startlevel` attribute and will therefore be assigned to start level 13. This value is picked because max(10,12)= 12 + 1 = 13.

Startlevels are assigned before the framework is started, they are updated on the fly if during a debug session the setup changes.

Normally in OSGi the begining start level is selected with the system property `org.osgi.framework.startlevel.beginning`. If
this `-runproperty` is not set then the launcher will set this property before starting the framework to the maximum of
specified levels + 2. If a start level management agent is present then this property should be set, the launcher will
then not interfere.

The bundles are started at during start level 1.

For example, a management agent that manages start levels is placed in start level 1 and all other bundles are placed
at start level 100.

	-runbundles: \
		org.apache.felix.configadmin;version='[1.8.8,1.8.9)',\
		org.apache.felix.http.jetty;version='[3.2.0,3.2.1)';startlevel=100,\
		org.apache.felix.http.servlet-api;version='[1.1.2,1.1.3)',\
		org.example.management.agent;startlevel=1
		osgi.enroute.twitter.bootstrap.webresource;version='[3.3.5,3.3.6)';startlevel=100,\
		osgi.enroute.web.simple.provider;version='[2.1.0,2.1.1)'


The `-runproperties` specify a begining of 1:

    -runproperties: \
           org.osgi.framework.startlevel.beginning=1

The launcher will install all bundles and assign them their start level. The framework is then started and moves
to level 1. This starts the management agent. This management agent will then move the start level higher to finally
level 100.

### Packaging

Any bndrun file can be packaged by the launcher plugin. This creates an executable JAR that will contain all its dependencies.

    $ bnd package xyz.bndrun

You can then execute the jar:

    $ java -jar xyz.jar

You can override all the properties that are embedded in the executable JAR with the `-Dlauncher.properties=<file>` option of the Java VM. Instead of consulting its internal properties file, the launcher will read the given file. However, you can also set individual System properties with the `-D` option that override a specific launching constant.

* `launch.storage.dir` – (`-runstorage`) Sets the directory for the framework directory. When `org.osgi.framework.storage` is also set, then the OSGi property has priority.
* `launch.keep` – Keeps the OSGi Framework storage around
* `launch.system.packages` – Extra system packages 
* `launch.system.capabilities` – Extra system capabilities
* `launch.trace` – Trace the launcher's progress
* `launch.timeout` – Abort after `timeout` milliseconds
* `launch.name` – Name of the executable (normally project name)
* `launch.noreferences` – Do not use the `reference:` scheme (`-runnoreferences`)
* `launch.notificationPort` – A port to send errors to

For example, if you want to run your executable in trace mode:

    $ java -Dlaunch.trace=true xyz.jar

### Expanding Executable JARs

An executable JAR can be unzipped in a directory. It will then include a Windows and Linuxy shell script to start the executable in expanded form. In the expanded form, the framework will try to use `reference:` URLs to install bundles when not on Windows. (Windows and reference URLs do not work well because Windows keeps files locked.)

If no reference URLs should be used, the `-runnoreferences=true` instruction can be set.

### Properties

Framework properties can be set using the `-runproperties` instruction.	 Framework properties do not include the system properties but any property set using `-runproperties` can be overridden with a system property. That is, it is impossible to set a property using the `-D` on the Java command line unless it was prior given a default in the bndrun file.

For example:

	bndrun:
	-runproperties: foo=3
	
	
	$ java -Dfoo=4 -Dbar=1 xyz.jar

In this example, the xyz app will see `foo=4` but `bar` will not be a framework property.

### Exit codes

If the framework stops the launcher will exit. It will set a system exit code that reflects the event type from that the framework returned. The shell script that started the launcher can take the system exit code into account to restart the framework in certain cases.

	OSGi FrameworkEvent             Launcher Constant  Value
	STOPPED                         STOPPED            -9
	WAIT_TIMEDOUT	                TIMEDOUT           -3
	ERROR                           ERROR              -2
	WARNING                         WARNING            -1
	STOPPED_BOOTCLASSPATH_MODIFIED  STOPPED_UPDATE     -4
	STOPPED_UPDATE                  STOPPED_UPDATE     -4

The launcher therefore returns the process exit code UPDATE_NEEDED(-4) when it requires an update. This was chosen over doing an in-process update because it is much safer. So the launching script should look something like:

	do {
	  bnd run app.bndrun
	} while ($?==-4)

## Remote Launching

The purpose of the aQute Remote project is to provide remote debugging support for bnd projects. It can be used to debug bundles and bndrun files in a remote machine running an OSGi framework with an agent installed on it; it can also install a framework on a remote machine before it uses the agent. The architecture is heavily optimized to run on small remote machines.

### Parts

This project is the bnd remote launcher. It consists of the following artifacts:

* `biz.aQute.remote.launcher` – The actual launcher. You can use the `biz.aQute.remote.launcher` by placing it on the `-runpath`. This will override the default launcher. It contains the agent and the bnd launcher plugin.
* `biz.aQute.remote.agent` – The agent that must run as a bundle on a framework, or alternatively, runs on the framework side of the classpath and uses the framework's Bundle Context. That is, you can put this file on the `-runpath` of the `biz.aQute.launcher` bndrun and bnd files.
* `biz.aQute.remote.main` – An executable JAR.

### Usage

The `biz.aQute.remote.launcher` should be placed on the `-runpath` in a bnd or bndrun file. The remote plugin expects a `-runremote` instruction in the parent file. This property has the following syntax:

	-runremote ::= remote (',' remote)*
	remote     ::= NAME ( ';' aspect '=' value ) *
	aspect	   ::= 'jdb' | 'shell' | 'host' | 'agent' | `timeout`
	
It is possible to specity multiple remote clauses. All sections are started simultaneously. The aspects are described in the following sections:

* `jdb` – The JDB debug port. This is the port that will be opened by the debugger in the IDE. If this aspect is not set, a number is assigned from 16043, incremented for every session. The debugger will wait until this port becomes available on the required host.
* `host` – The name of the remote host. Default `localhost`. Notice that for some Unixes there is a confusion what localhost is, some Linux variants make localhost 127.0.1.1 while the original is 127.0.0.1. So better make sure.
* `shell` – The shell that should be used. There are a number of possibilities for this value
	* 0 – No shell. The IDE shell will not be used
	* < 0 – Try to use an available Gogo shell in the framework and open a session. The input/output of the session is forwarded to the IDE console.
	* 1 – Use the System.in/System.out/System.err streams. This will replace the existing streams (this can be prevented with a security manager). This mode will forward all IO to the original streams.
	* > 1 – A TCP port. The launcher will attach the port from the remote host and forward any I/O.
* `agent` – The port on which the agent is listening, the default is ${aQute.agent.server.port}.
* `timeout` – Timeout in seconds for the debug connection

### Example bndrun

An example remote bndrun file:

	local		=	\
		local; \
		shell   =   4003; \
		jdb     =   1044; \
		host    =   localhost
	
	-runremote:     ${local}	
	-runfw:         org.apache.felix.framework;version='[4,5)'
	-runee:         JavaSE-1.8
	-runproperties: gosh.args=--noshutdown, osgi.shell.telnet.port=4003
	
	-runpath:       biz.aQute.remote.launcher;version=latest
	-runrequires:\
		osgi.identity;filter:='(osgi.identity=org.apache.felix.gogo.shell)',\
		osgi.identity;filter:='(osgi.identity=org.apache.felix.gogo.command)'
	
	-runbundles:\
		launch.simple.really;version=latest, \
		org.apache.felix.gogo.shell,\
		org.apache.felix.gogo.command,\
		org.apache.felix.gogo.runtime, \
		org.apache.felix.shell.remote
