---
layout: default
title: -javaagent BOOLEAN
class: Project
summary: |
   Specify if classpath jars with Premain-Class headers are to be used as java agents
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-javaagent: true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/javaagent.md --><br /><br />

If the value is true, then each classpath elements that
has a `Premain-Class` manifest header is used as a java agent
when launching by adding a `-javaagent:` argument to the java invocation.

    -javaagent:jarpath

If the classpath element was specified with an `agent` attribute, the
value of the `agent` attribute will be used as the options for the
`-jaragent:` argument to the java invocation.

    -javaagent:jarpath=options
