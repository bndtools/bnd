---
layout: default
title: -init ${MACRO} ( ',' ${MACRO}) *
class: Project
summary: |
   Executes the macros while initializing the project for building.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-init: ${my_macro} `

- Pattern: `.*`

<!-- Manual content from: ext/init.md --><br /><br />

You can use `-init` as follows:

	-init: ${my_macro}, ${my_macro2}

Macros are usually resolved and executed on demand. `-init` always executes the macros while initializing the project for building.
