---
layout: default
class: Workspace
title: -runprovidedcapabilities 
summary: Extra capabilities for a distro resolve
---

# -runprovidedcapabilities

The `-runprovidedcapabilities` instruction is used with the `-distro` instruction to specify extra capabilities that are provided by the target system but not listed in the distro file. This helps the resolver understand what is available at runtime.


The `-runprovidedcapabilities` instruction allows you to declare additional capabilities for a distribution that are not explicitly mentioned in the distribution file. By using this instruction, you enable the resolver to take into account these extra capabilities when determining the set of available features and functionalities in the target environment. This can be particularly useful in scenarios where certain capabilities are provided by the system but are not included in the default distribution configuration.

Example:

```
-runprovidedcapabilities: some.namespace;some.namespace=foo
```

This instruction is only relevant when working with distribution-based resolves.



---
TODO Needs review - AI Generated content