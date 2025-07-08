---
layout: default
title: -runsystemcapabilities* CAPABILITY (',' CAPABILITY )
class: Launcher
summary: |
   Define extra capabilities for the remote VM.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runsystemcapabilities=some.namespace; some.namespace=foo`

- Pattern: `.*`

<!-- Manual content from: ext/runsystemcapabilities.md --><br /><br />
Although resolver analyses the `-runpath` (and thus `-runfw`) for system capabilities, this is not always sufficient. For example, if the target system has special hardware then this might be described with a capability. Such an external capability must be explicitly given to the resolver. These _extra_ capabilities maybe given with the `-runsystemcapabilities` instruction.

For example:

	-runsystemcapabilities: \
		some.namespace; \
			some.namespace=foo
