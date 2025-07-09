---
layout: default
title: -jpms-multi-release BOOLEAN
class: JPMS
summary: |
   Enables generating manifests and module infos for multi release JARs.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-jpms-multi-release: true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/jpms_multi_release.md --><br /><br />

This instruction controls that if a JAR setup to be a [multi-release jar][1] the manifests & module-infos 
for each supported versions should be added. If this instruction is true, it will generate this metadata,
if the instruction is absent or the value is not true, then it will ignore the `versions`.

A multi release Jar (MRJ) will contain directories in `META-INF/versions/`, where the directory name is a release
number. If this instruction is enabled, then during manifest generation, bnd will also calculate a manifest and
module-info in each versioned directory.

[1]: https://docs.oracle.com/en/java/javase/17/docs/specs/jar/jar.html
