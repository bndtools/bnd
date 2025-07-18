---
layout: default
title: -runprovidedcapabilities
class: Workspace
summary: |
   Extra capabilities for a distro resolve
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runprovidedcapabilities=some.namespace; some.namespace=foo`

- Pattern: `.*`

<!-- Manual content from: ext/runprovidedcapabilities.md --><br /><br />

# -runprovidedcapabilities

The `-runprovidedcapabilities` instruction is used with the `-distro` instruction to specify extra capabilities that are provided by the target system but not listed in the distro file. This helps the resolver understand what is available at runtime.


The `-runprovidedcapabilities` instruction allows you to declare additional capabilities for a distribution that are not explicitly mentioned in the distribution file. By using this instruction, you enable the resolver to take into account these extra capabilities when determining the set of available features and functionalities in the target environment. This can be particularly useful in scenarios where certain capabilities are provided by the system but are not included in the default distribution configuration.

Example:

```
-runprovidedcapabilities: some.namespace;some.namespace=foo
```

This instruction is only relevant when working with distribution-based resolves.



TODO Needs review - AI Generated content
