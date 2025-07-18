---
layout: default
class: Processor
title: -pluginpath* PARAMETERS 
summary: Define JARs to be loaded in the local classloader for plugins. 
---

The `-pluginpath` instruction allows you to define JAR files or directories that should be loaded into the local classloader for plugins. This is useful for plugins that are not embedded in bndlib and need to load their classes from external locations.

You can specify multiple `-pluginpath` clauses in different places, and they will be merged together. If a specified file does not exist, you can provide a URL to download it, and optionally a SHA-1 digest to verify the download. This ensures that all required plugin dependencies are available and loaded correctly at build time.

---
TODO Needs review - AI Generated content