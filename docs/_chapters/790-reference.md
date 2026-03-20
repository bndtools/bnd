---
title: Reference
layout: bnd
parent: Reference Material
nav_order: 1
---
The subsequent sections provide the reference part of the manual. This consists of the following:

Since bnd is a library, it gets used in many different places. This makes some of the headers, instructions, and/or
macros only applicable in a specific context. This is generally indicated on a page with a _class_ button on the right
side. For example:

<div class="pageclass" style="float:none;margin:12px">
Project
</div>

The following classes are used:

* **Workspace** – A workspace model (not maven or gradle)
* **Project** – A workspace model in the context of a project
* **Analyzer** – Can be used in the analysis & builder phase
* **Macro** – General macro, applicable everywhere
* Maven, Ant, Gradle – Only applicable in the given tool

Usually the format of both headers and instructions follows the Java properties files specification outlined in [Concepts](130-concepts.html).