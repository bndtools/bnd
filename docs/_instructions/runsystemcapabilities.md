---
layout: default
class: Launcher
title: -runsystemcapabilities* CAPABILITY (',' CAPABILITY ) 
summary:  Define extra capabilities for the remote VM.
---
Although resolver analyses the `-runpath` (and thus `-runfw`) for system capabilities, this is not always sufficient. For example, if the target system has special hardware then this might be described with a capability. Such an external capability must be explicitly given to the resolver. These _extra_ capabilities maybe given with the `-runsystemcapabilities` instruction.

For example:

	-runsystemcapabilities: \
		some.namespace; \
			some.namespace=foo
