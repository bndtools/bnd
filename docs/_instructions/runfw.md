---
layout: default
title: -runfw REPO-ENTRY
class: Launcher
summary: |
   Specify the framework JAR's entry in a repository.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runfw: org.eclipse.osgi; version=3.10`

- Pattern: `.*`

<!-- Manual content from: ext/runfw.md --><br /><br />

The `-runfw` instruction sets the framework to use. This framework will be added to the `-runpath`. Any exported packages or capabilities listed in the manifest of the framework are automatically added to the system capabilities.

For example:

	-runfw: org.eclipse.osgi; version=3.10

**Note** – Do not use `runframework`, this instruction is deprecated and had very different intent and syntax.
