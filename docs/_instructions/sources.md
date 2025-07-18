---
layout: default
title: -sources  BOOLEAN
class: Builder
summary: |
   Include the source code (if available on the -sourcepath) in the bundle at OSGI-OPT/src
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-sources=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/sources.md --><br /><br />

The `-sources` instruction tells bnd to include the source code (if available on the `-sourcepath`) in the bundle under the `OSGI-OPT/src` directory. This is useful for distributing source code along with your binary bundle, making it easier for others to debug or understand your code.

When enabled, bnd will collect the relevant source files and package them in the appropriate location within the bundle.

	

TODO Needs review - AI Generated content
