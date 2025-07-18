---
layout: default
class: Builder
title: -manifest FILE   
summary:  Override manifest calculation and set fixed manifest
---

The `-manifest` instruction allows you to override the default manifest calculation and specify a fixed manifest file to use in your JAR. When this instruction is set, bnd will use the provided manifest file instead of generating one automatically, although it will still analyze the classpath as part of the build process.

This is useful when you need to comply with specific manifest requirements or reuse an existing manifest file for your bundle.

---
TODO Needs review - AI Generated content