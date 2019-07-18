---
layout: default
class: Macro
title: template ';' NAME [ ';' template ]+
summary: Expand the entries of a merged and decorated Parameters object using a template that can refer to the key and attributes
---

The `template` macro is intended to make it very simple to generate new information based on a _Parameters_. Parameters 
are the OSGi/bnd workhorse to store information. The macro takes the following arguments:

    macro name  The name of the macro (not the value)
    template+   the templates (these are  joined with the ';' as separator)

The template is expanded for each entry of the Parameters. They key can be referred by `${@}` and the attributes can be
referred by `${@<name>}`, where the name is the name of the attribute. All entries are then joined with a comma (`,`) as 
separator. 
 
For example, the following example shows how to extract and attribute as a list:

    bnd shell
    > parameters = key;attr=1, key;attr="2"
    > ${template;parameters;${@attr}}
    1,2    

The `template` macro takes the `NAME` of the macro that contains the value. I.e. it does not take the expanded value as 
argument.  The reason  is that the referred macro gets _merged_ and _decorated_. Merge takes all properties that start with the given name. 

    > parameters = key;attr=1, key;attr="2"
    > parameters.extra = KEY;attr=3, KEY;attr="4"
    > ${template;parameters;${@attr}}
    1,2,3,4    
    
Decorate means that the property with the same name but with a `+` at the end will be matched with the value. In this property the
key is a _glob_. It is matched against the key from the original merged properties. Any matching properties get the attributes
from the decorator.

    > parameters = a;attr=1, b;attr="2"
    > parameters.extra = c;attr=3, d;attr="4"
    > parameters+ = {c,d};attr=X
    > ${template;parameters;${@attr}}
    1,2,X,X    

The macro accepts any number of arguments after the macro name. These values are joined with a semi-colon as separator.
The reason is that then the `;` in the templates do not have to be escaped:

    bnd shell
    > parameters = a;attr=1, b;attr="2"
    > ${template;parameters;${@};key=${@};${@}=${@attr}}
    a;a=1,b;b=2    




