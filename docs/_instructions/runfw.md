---
layout: default
class: Launcher
title: -runfw REPO-ENTRY
summary: Specify the framework JAR's entry in a repository.
---

The `-runfw` instruction sets the framework to use. This framework will be added to the `-runpath`. Any exported packages or capabilities listed in the manifest of the framework are automatically added to the system capabilities.

For example:

	-runfw: org.eclipse.osgi; version=3.10

**Note** – Do not use `runframework`, this instruction is deprecated and had very different intent and syntax.
