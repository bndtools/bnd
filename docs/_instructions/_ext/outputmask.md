---
layout: default
class: Project
title: -outputmask  TEMPLATE ?
summary: If set, is used a template to calculate the output file. It can use any macro but the ${@bsn} and ${@version} macros refer to the current JAR being saved. The default is bsn + ".jar".  
---

The `-outputmask` instruction allows you to define a template for naming the output file when building a JAR. You can use any macro in the template, but `${@bsn}` and `${@version}` are especially useful as they refer to the current bundle symbolic name and version, respectively. The default template is `${@bsn}.jar`.

This instruction is helpful for customizing the naming convention of your build artifacts, making it easier to organize and identify them according to your project's needs.


<hr />
TODO Needs review - AI Generated content