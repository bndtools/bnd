---
layout: default
class: Builder
title: -noextraheaders  BOOLEAN
summary:  Do not add a any extra headers specific for bnd. 
---

The `-noextraheaders` instruction controls whether bnd adds extra headers to the manifest that are specific to bnd, such as `Created-By`, `Tool`, and `Bnd-LastModified`. By default, these headers are included to provide information about the build environment and tool versions. When this instruction is set to `true`, bnd will not add these extra headers, resulting in a cleaner manifest with only the standard OSGi headers.

This can be useful if you want to minimize metadata or ensure that your manifest contains only the required information.


---
TODO Needs review - AI Generated content