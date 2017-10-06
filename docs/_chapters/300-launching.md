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

	JPM-Command: xray
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
	
	-runrequire:\
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

The launcher supports _embedded activators_. These are like normal Bundle Actviator classes but are instead found on the `-runpath`. Any bundle that has the header `Embedded-Activator` will be started. This starting can happen before any bundle is started or after all bundles are started. To start immediate, add a static or instance field called `IMMEDIATE` with a value that equals `true`.

    public class MyActivator implements BundleActivator {
        public static boolean IMMEDIATE = true;

        public void start(BundleContext context) {}
        
        public void stop(BundleContext context) {}
    }

    In the manifest:

    Embedded-Activator: com.example.MyActivator

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
* `biz.aQute.remote.main` – An executable JAR compatible with JPM. (That is, you can install it from jpm with `sudo jpm install biz.aQute.remote.main` or `sudo jpm install bndremote`.

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
