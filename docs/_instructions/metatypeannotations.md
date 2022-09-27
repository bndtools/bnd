---
layout: default
class: Builder
title: -metatypeannotations SELECTORS
summary:  Selects the packages that need processing for standard OSGi Metatype annotations. 
---

The `-metatypeannotations` instruction tells **bnd** which bundle classes, if any, to search for [Metatype Service](https://osgi.org/specification/osgi.cmpn/7.0.0/service.metatype.html) annotations. **bnd** will then process those classes into Metatype XML resources.

The value of this instruction is a comma delimited list of fully qualified class names.

The default value of this instruction is `*`, which means that by default **bnd** will process all bundle classes looking for Metatype annotations.

The behavior of Metatype annotation processing can be further configured using the [-metatypeannotations-options](metatypeannotations-options.html) instruction.

[source](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib/src/aQute/bnd/metatype/MetatypeAnnotations.java)