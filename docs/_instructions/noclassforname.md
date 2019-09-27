---
layout: default
class: Builder
title: -noclassforname BOOLEAN
summary: Do not add package reference to classes loaded with Class.forName(String).
---

Normally Bnd will examine the method bodies of classes looking for the instruction sequence:

    ldc(_w) "some.Class"
    invokestatic "java/lang/Class" "forName(Ljava/lang/String;)Ljava/lang/Class;"

which results from calls to `Class.forName(String)` passing a String constant for the class name. Bnd will use the String constant as a class reference for the purposes of calculating package references for generating the Import-Package manifest header.

The `-noclassforname` instruction can be used to tell Bnd to not search the method bodies for this instruction sequence.

For example:

	-noclassforname: true
