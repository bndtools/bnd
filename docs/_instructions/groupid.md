---
layout: default
title: -groupid groupId
class: Project
summary: |
   Set the default Maven groupId
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-groupid=com.foo.bar`

- Pattern: `.*`

<!-- Manual content from: ext/groupid.md --><br /><br />

The `-groupid` instruction defines the default Maven groupId to be used when generating POM resources and releasing to the [Maven Bnd Repository Plugin][1].

If not specified, the value of `-groupid` defaults to the Bnd workspace folder name.

Also see the [-pom][2] and [-maven-release][3] instructions.

[1]: /plugins/maven
[2]: /instructions/pom.html
[3]: /instructions/maven_release.html
