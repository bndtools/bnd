---
layout: bnd
title: javac.release
class: Header
summary: |
   Compiles against the documented Java SE and JDK API of the specified release and generates class files for that release using javac --release. This prevents accidental use of newer platform APIs. It takes precedence over javac.source and javac.target. It cannot be combined with javac.profile or a bootclasspath. Set to an empty value to force legacy -source/-target behaviour.
parent: Headers
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `javac.release: 17`

- Pattern: `.*`

