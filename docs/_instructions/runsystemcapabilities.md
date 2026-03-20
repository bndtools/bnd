---
layout: bnd
title: -runsystemcapabilities* CAPABILITY (',' CAPABILITY )
class: Launcher
summary: |
   Define extra capabilities for the remote VM.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
parent: Instruction Reference
---
- Example: `-runsystemcapabilities=some.namespace; some.namespace=foo`

- Pattern: `.*`

<!-- Manual content from: ext/runsystemcapabilities.md --><br /><br />
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
