---
layout: default
class: Project
title: -javaagent BOOLEAN
summary:  Specify if classpath jars with Premain-Class headers are to be used as java agents
---

If the value is true, then each classpath elements that
has a `Premain-Class` manifest header is used as a java agent
when launching by adding a `-javaagent:` argument to the java invocation.

    -javaagent:jarpath

If the classpath element was specified with an `agent` attribute, the
value of the `agent` attribute will be used as the options for the
`-jaragent:` argument to the java invocation.

    -javaagent:jarpath=options
