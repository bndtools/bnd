---
layout: default
class: Analyzer
title: -noimportjava BOOLEAN
summary: Do not import java.* packages.
---

Prior to OSGi Core R7, it was invalid for the Import-Package header to include `java.*` packages. So Bnd would never include them in the generated Import-Package header. In [OSGi Core R7](https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html#framework.module-execution.environment), or later, it is now permitted to include `java.*` packages in the Import-Package header. This allows the OSGi framework validate the execution environment can supply all the java packages required by a bundle. This can avoid a `NoClassDefFoundError` during execution of the bundle due to a missing `java.*` package required by the bundle. 

Bnd will now generate the Import-Package header including referenced `java.*` packages when either the bundle imports the `org.osgi.framework` package from OSGi Core R7, or later, or when the bundle includes class files targeting Java 11, or later.

The `-noimportjava` instruction can be used to tell Bnd not to include referenced `java.*` packages in the generated Import-Package header.

For example:

	-noimportjava: true

You can use the `Import-Package` instruction to control which referenced `java.*` packages should be imported.

For example:

	Import-Package: java.util.*, !java.*, *

will only import `java.util.*` packages and no other `java.*` packages.

