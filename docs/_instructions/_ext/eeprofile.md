---
layout: default
class: Project
title: -eeprofile 'auto' | PROFILE +
summary: Provides control over what Java 8 profile to use.
---
	


The `-eeprofile` instruction allows you to control which Java 8 (or later) profile is used for your project. A profile is a set of Java packages that enables the Java Virtual Machine to be delivered in smaller, more targeted versions.

You can set `-eeprofile` to `auto` to let bnd automatically determine the appropriate profile based on the packages your project uses. Alternatively, you can specify one or more profiles explicitly by name. If your project references packages outside the selected profiles, no profile will be set.

This instruction is useful for optimizing your bundle for specific Java environments and ensuring compatibility with the intended runtime profile.

---
TODO Needs review - AI Generated content