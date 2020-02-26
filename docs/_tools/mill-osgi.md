---
title: Mill OSGi Plugin
layout: default
summary: A Mill Plugin to create OSGi Bundles.
version: 0.2.0
---

Plugin to build OSGi bundles with the [mill build tool][1].

## Quickstart

```scala
// mill default imports
import mill._, scalalib._
// Load mill-osgi in version 0.2.0
import $ivy.`de.tototec::de.tobiasroeser.mill.osgi:0.2.0`
// and import its main package
import de.tobiasroeser.mill.osgi._

object project extends JavaModule with OsgiBundleModule {

  def bundleSymbolicName = "com.example.project"

  def osgiHeaders = T{ osgiHeaders().copy(
    `Export-Package`   = Seq("com.example.api"),
    `Bundle-Activator` = Some("com.example.internal.Activator")
  )}

}
```

## Download and Documentation

Please refer the [plugin project page][2] for the full documentation and download details.

[1]: https://github.com/lihaoyi/mill
[2]: https://github.com/lefou/mill-osgi
