---
layout: default
title: -metatypeannotations SELECTORS
class: Builder
summary: |
   Selects the packages that need processing for standard OSGi Metatype annotations.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-metatypeannotations: *`

- Pattern: `.*`

<!-- Manual content from: ext/metatypeannotations.md --><br /><br />

The `-metatypeannotations` instruction tells **bnd** which bundle classes, if any, to search for [Metatype Service](https://osgi.org/specification/osgi.cmpn/8.0.0/service.metatype.html) annotations. **bnd** will then process those classes into Metatype XML resources.

The value of this instruction is a comma delimited list of fully qualified class names.

The default value of this instruction is `*`, which means that by default **bnd** will process all bundle classes looking for Metatype annotations.

The behavior of Metatype annotation processing can be further configured using the [-metatypeannotations-options](metatypeannotations_options.html) instruction.

[source](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib/src/aQute/bnd/metatype/MetatypeAnnotations.java)
