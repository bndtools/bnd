# REMOTE
### Since 3.0.0

## Purpose

The purpose of the aQute Remote project is to provide remote debugging support for bnd projects. It can be used to debug bundles and bndrun files in a remote machine running an OSGi framework with an agent installed on it; it can also install a framework on a remote machine before it uses the agent. The architecture is heavily optimized to run on small remote machines.

## Parts

This project is the bnd remote launcher. It consists of the following artifacts:

* `biz.aQute.remote.launcher` – The actual launcher. You can use the `biz.aQute.remote.launcher` by placing it on the `-runpath`. This will override the default launcher. It contains the agent and the bnd launcher plugin.
* `biz.aQute.remote.agent` – The agent that must run as a bundle on a framework, or alternatively, runs on the framework side of the classpath and uses the framework's Bundle Context. That is, you can put this file on the `-runpath` of the `biz.aQute.launcher` bndrun and bnd files.
* `biz.aQute.remote.main` – An executable JAR compatible with JPM. (That is, you can install it from jpm with `sudo jpm install biz.aQute.remote.main` or `sudo jpm install bndremote`.

## Usage

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

 

