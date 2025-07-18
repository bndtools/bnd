---
layout: default
class: Ant
title: -nomanifest  BOOLEAN
summary:  Do not safe the manifest in the JAR.
---

# -nomanifest

The `-nomanifest` instruction controls whether the manifest file is included in the generated JAR. When set to `true`, the manifest will not be saved in the JAR file. This can be useful in scenarios where a manifest is not required or when you want to minimize the contents of the JAR for specific use cases.

Typically, most OSGi bundles require a manifest, but in some advanced or custom build scenarios, you may wish to omit it. Use this option with caution, as omitting the manifest may cause the resulting JAR to be unusable in standard OSGi environments.

By default, the manifest is included in the JAR unless `-nomanifest: true` is specified.


<hr />
TODO Needs review - AI Generated content