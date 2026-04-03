---
layout: default
class: Builder
title: -preprocessmatchers SELECTOR 
summary: Specify which files can be preprocessed 
---

During the [-includeresource](includeresource.html) processing it is possible to pre-process the files that are copied into the JAR by enclosing the clause in curly braces (`{}`). Since this can create havoc when applied to text files bnd will attempt to skip _binary_ files. To skip binary files, bnd uses a pre-process matchers list. This list is a standard _selector_. The default is:
 
    !*.(jpg|jpeg|jif|jfif|jp2|jpx|j2k|j2c|fpx|png|gif|swf|doc|pdf|tiff|tif|raw|bmp|ppm|pgm|pbm|pnm|pfm|webp|zip|jar|gz|tar|tgz|exe|com|bin|mp[0-9]|mpeg|mov|):i, *
    
When bnd copies a file from a source to a directory it will match that name against this list. If it is one of the extensions listed, then it will not preprocess that file.

The default can be overridden with the `-preprocessmatchers` instruction.

    -preprocessmatchers:    !OSGI-INF/*,* 

