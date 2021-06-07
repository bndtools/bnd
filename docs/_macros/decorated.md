---
layout: default
class: Macro
title: decorated ';' NAME [ ';' BOOLEAN ] ?
summary: The merged and decorated Parameters object
---

The `decorated` macro is intended to make it very simple to decorate information based on a _Parameters_. Parameters 
are the Bnd workhorse to store information. The macro takes the following arguments:

    name    The name of the macro (not the value)
    boolean Whether to add unused literals. Defaults to false.

Decorate means that the property with the same name but with a `+` at the end will be matched with the value. In this property the
key is a _glob_. It is matched against the key from the original merged properties. Any matching properties get the attributes
from the decorator.

    > parameters=a,b
    > parameters.extra=c,d
    > parameters+=(c|d);attr=X
    > ${decorated;parameters}
    a,b,c;attr=X,d;attr=X

    > parameters=a,b
    > parameters.extra=c,d
    > parameters+=(c|d);attr=X,x,y,z
    > ${decorated;parameters;true}
    a,b,c;attr=X,d;attr=X,x,y,z
