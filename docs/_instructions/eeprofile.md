---
layout: default
title: -eeprofile 'auto' | PROFILE +
class: Project
summary: |
   Provides control over what Java 8 profile to use.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-eeprofile: name="a,b,c"`

- Values: `name="a,b,c", auto`

- Pattern: `.*`

<!-- Manual content from: ext/eeprofile.md --><br /><br />
	


The `-eeprofile` instruction allows you to control which Java 8 (or later) profile is used for your project. A profile is a set of Java packages that enables the Java Virtual Machine to be delivered in smaller, more targeted versions.

You can set `-eeprofile` to `auto` to let bnd automatically determine the appropriate profile based on the packages your project uses. Alternatively, you can specify one or more profiles explicitly by name. If your project references packages outside the selected profiles, no profile will be set.

This instruction is useful for optimizing your bundle for specific Java environments and ensuring compatibility with the intended runtime profile.

<hr />
TODO Needs review - AI Generated content
