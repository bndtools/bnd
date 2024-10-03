---
layout: default
class: Project
title: -init ${MACRO} ( ',' ${MACRO}) * 
summary:  Executes the macros while initializing the project for building.
---

You can use `-init` as follows:

	-init: ${my_macro}, ${my_macro2}

Macros are usually resolved and executed on demand. `-init` always executes the macros while initializing the project for building.
