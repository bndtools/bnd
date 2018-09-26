---
layout: default
class: Macro
title: apply ';' MACRO (';' LIST)* 
summary: Convert a list to an invoction with arguments 
---

The `apply` macro takes the name of a macro and invokes this macro with each element of the given list as an argument. It is useful when the arguments to the macro are in a comma separated list.
	
	args = com.example.foo, 3.12, HIGHEST
	${apply;repo;${args}}

This will be expanded to:

	${repo;com.example.foo;3.12;HIGHEST}

Which will provide the path to the artifact
