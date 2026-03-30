---
layout: default
class: Builder
title: -dsannotations SELECTORS
summary: Selects the packages that need processing for standard OSGi DS annotations.
---

The `-dsannotations` instruction tells bnd which classes in your bundle should be scanned for Declarative Services (DS) annotations. bnd will process these classes and generate the necessary DS XML descriptors automatically.

You provide a comma-separated list of fully qualified class names or use `*` to process all classes. This makes it easy to enable DS annotation processing for your entire bundle or for specific classes only.

You can further configure DS annotation processing using the `-dsannotations-options` instruction.

## Component Class Requirements

When bnd processes DS annotations, it validates that component classes meet the DS specification requirements. Starting with bnd 7.3.0, component classes must have either a public no-argument constructor or a public `@Activate`-annotated constructor. Inner classes must be declared as `static`.

If validation fails, bnd will generate an error and stop the build. See [Component Class Requirements](/chapters/200-components.html#component-class-requirements) for detailed requirements and examples.

[source](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib/src/aQute/bnd/component/DSAnnotations.java)

<hr />
TODO Needs review - AI Generated content