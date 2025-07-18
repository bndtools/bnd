---
layout: default
title: -systemproperties PROPERTIES
class: Workspace
summary: |
   These system properties are set in the local JVM when a workspace is started. This was mainly added to allow one to set JVM options via system properties.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-systemproperties= foo=3, bar=4`

- Pattern: `.*`

<!-- Manual content from: ext/systemproperties.md --><br /><br />

The `-systemproperties` instruction allows you to set system properties in the local JVM when a workspace is started. These properties are applied as early as possible, making it possible to configure JVM options or work around specific Java issues (such as SSL settings) before any other code runs.

This is especially useful for setting environment-specific options or JVM flags that must be in place for your workspace or build to function correctly.

<hr />
TODO Needs review - AI Generated content
