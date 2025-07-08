---
layout: default
title: -bundleannotations SELECTORS
class: Project
summary: |
   Selects the classes that need processing for standard OSGi Bundle annotations.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-bundleannotations: com.foo.bar.MyClazz`

- Pattern: `.*`

<!-- Manual content from: ext/bundleannotations.md --><br /><br />

The `-bundleannotations` instruction tells **bnd** which bundle classes, if any, to search for [OSGi Bundle annotations](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle). **bnd** will then process those annotations into manifest headers.

The value of this instruction is a comma delimited list of fully qualified class names.

The default value of this instruction is `*`, which means that by default **bnd** will process all bundle classes looking for Bundle annotations.

[source](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib/src/aQute/bnd/osgi/AnnotationHeaders.java)
