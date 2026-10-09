---
layout: default
class: Analyzer
title: -noimportjava BOOLEAN
summary: Do not import java.* packages.
---

Prior to OSGi Core R7, it was invalid for the Import-Package header to include `java.*` packages. So Bnd would never include them in the generated Import-Package header. In OSGi Core R7, or later (e.g. [OSGi Core R8](https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html#framework.module-execution.environment)), it is now permitted to include `java.*` packages in the Import-Package header. This allows the OSGi framework validate the execution environment can supply all the java packages required by a bundle. This can avoid a `NoClassDefFoundError` during execution of the bundle due to a missing `java.*` package required by the bundle. 

Bnd since version 7.0.0 generates the Import-Package header including referenced `java.*` packages automatically except if

- the bundle references package `org.osgi.framework` with lower version bound less than 1.9/R7 (i.e. is equal to the bnd classpath being populated with a version lower than 1.9), or
- the instruction `-noimportjava` is set to `true`

You can use the `Import-Package` instruction to control which referenced `java.*` packages should be imported.

For example:

	Import-Package: java.util.*, !java.*, *

will only import `java.util.*` packages and no other `java.*` packages.

