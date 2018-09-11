---
layout: default
class: Project
title: -dsannotations SELECTORS
summary: Selects the packages that need processing for standard OSGi DS annotations. 
---

The `-dsannotations` instruction tells **bnd** which bundle classes, if any, to search for [Declarative Services (DS)](https://osgi.org/specification/osgi.cmpn/7.0.0/service.component.html) annotations. **bnd** will then process those classes into DS XML descriptors.

The value of this instruction is a comma delimited list of fully qualified class names.

The default value of this instruction is `*`, which means that by default **bnd** will process all bundle classes looking for DS annotations.

The behavior of DS annotation processing can be further configured using the [-dsannotations-options](./dsannotations-options.md) instruction.

[source](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib/src/aQute/bnd/component/DSAnnotations.java)