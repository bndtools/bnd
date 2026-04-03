---
layout: default
class: Launcher
title: -runsystemcapabilities* CAPABILITY (',' CAPABILITY ) 
summary:  Define extra capabilities for the remote VM.
---
Although resolver analyses the `-runpath` (and thus `-runfw`) for system capabilities, this is not always sufficient. For example, if the target system has special hardware then this might be described with a capability. Such an external capability must be explicitly given to the resolver. These _extra_ capabilities maybe given with the `-runsystemcapabilities` instruction.

- Examples: 

	explizitly use system capabilities (also to for viewing them ;-)
	```
	-runsystemcapabilities=${native_capability}
	```
	define specific e.g. windows host capabilities
	```
	-runsystemcapabilities=\
 		osgi.native; \
 		osgi.native.osname:List<String>="Windows10,Windows 10,Win32"; \
 		osgi.native.osversion:Version="10.0.0"; \
 		osgi.native.processor:List<String>="x86-64,amd64,em64t,x86_64"; \
 		osgi.native.language=en_US
 	```
	other capabilities
	```
	-runsystemcapabilities=some.namespace; some.namespace=foo
	```

- Pattern: `.*`


