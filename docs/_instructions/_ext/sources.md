---
layout: default
class: Builder
title: -sources  BOOLEAN
summary: Include the source code (if available on the -sourcepath) in the bundle at OSGI-OPT/src 
---

The `-sources` instruction tells bnd to include the source code (if available on the `-sourcepath`) in the bundle under the `OSGI-OPT/src` directory. This is useful for distributing source code along with your binary bundle, making it easier for others to debug or understand your code.

When enabled, bnd will collect the relevant source files and package them in the appropriate location within the bundle.

	