---
layout: default
title: -noee  BOOLEAN
class: Ant
summary: |
   Donot add an automatic requirement on an EE capability based on the class format.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-noee=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/noee.md --><br /><br />

The `-noee` instruction controls whether bnd automatically adds a requirement for an Execution Environment (EE) capability based on the class file format. By default, bnd will analyze the class version and add the minimum required EE as a requirement. When this instruction is set to `true`, bnd will not add this automatic requirement, giving you full control over EE requirements in your bundle.

This is useful if you want to manage EE requirements manually or if your project has special compatibility needs.


<hr />
TODO Needs review - AI Generated content
