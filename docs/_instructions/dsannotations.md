---
layout: default
title: -dsannotations SELECTORS
class: Builder
summary: |
   Selects the packages that need processing for standard OSGi DS annotations.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-dsannotations: *`

- Pattern: `.*`

<!-- Manual content from: ext/dsannotations.md --><br /><br />

The `-dsannotations` instruction tells bnd which classes in your bundle should be scanned for Declarative Services (DS) annotations. bnd will process these classes and generate the necessary DS XML descriptors automatically.

You provide a comma-separated list of fully qualified class names or use `*` to process all classes. This makes it easy to enable DS annotation processing for your entire bundle or for specific classes only.

You can further configure DS annotation processing using the `-dsannotations-options` instruction.

[source](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib/src/aQute/bnd/component/DSAnnotations.java)

<hr />
TODO Needs review - AI Generated content
