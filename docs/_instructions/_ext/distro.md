---
layout: default
class: Workspace
title: -distro REPO (',' REPO)
summary: Resolve against pre-defined system capabilities 
---

The `-distro` instruction is used in the case that your application must run in a host environment, for example Karaf. In such cases it is not possible to calculate the system capabilities from the framework and the run path. Each of these host environments has a specific set of capabilities that should be used as input to the resolver. 

The `-distro` capability has the same syntax as the `-runpath`, a list of bundle specifications. The resolver will parse these bundles and treat their capabilities specified with the `Provide-Capability` header in their Manifest as the system capabilities. These files can be generated manually. However, the bnd command line tool can create a distro bundle using the remote agent.

When the `-distro` is present in the `bndrun` file it overrides any other definition that are used to derive capabilities. If additional capabilities are needed the `-runprovidedcapabilities` should be used.

For example:

	-distro: karaf-4.1.jar;version=file
