---
layout: default
title: -nouses  BOOLEAN
class: Project
summary: |
   Do not calculate uses directives on package exports or on capabilities.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-nouses=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/nouses.md --><br /><br />

Do not calculate the [uses directive](https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html#i3127019) on package exports or on capabilities, if set to _true_.
Default: _false_

For example:

    -nouses: true


**Warning:** 
Setting this flag to _true_ is rarely needed and can be **dangerous**. Without any _uses_ clause, all packages are treated as independent from each other. That means the OSGi resolver is free to wire these packages to different classloaders if used by a consumer (or its dependencies).
