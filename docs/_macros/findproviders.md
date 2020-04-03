---
layout: default
class: Workspace
title: findproviders  ';' namespace ( ';' FILTER )? 
summary: find resources in the workspace repository matching the given namespace and optional filter. EXPERIMENTAL
---

The `findproviders` macro gives access to the resources in the Workspace repositories, including the projects itself. Its semantics match the OSGi `Repository.findProviders()` method.

It was added to support the use case of including a set of plugins in a resolve to create an executable. Since it is impossible to add a requirement that will add all matching resources, the `findproviders` macro can be used to find these resources and then turn them into initial requirements. However, this macro will likely find many other use cases since it is quite versatile.

    findproviders ::= namespace ( ';' FILTER )? 
    namespace     ::= ... OSGi namespace

If no filter is given then all resources that have a capability in the given namespace are returned. 

The result is in `PARAMETERS` format, to common format in bnd and OSGi. In this case the _key_ will be the Bundle Symbolic Name and the attributes will _at least_ contain the Bundle version. For example:

    ${findproviders;osgi.service}
    
This finds all resources that have an `osgi.service` capability. This could return something like:

    com.h2database;version=1.4.198,
    org.apache.aries.async;version=1.0.1,
    org.apache.felix.configadmin;version=1.9.12,
    org.apache.felix.coordinator;version=1.0.2,
    org.apache.felix.eventadmin;version=1.5.0,
    ...

This `PARAMETERS` format is _structured_ because it has attributes; that makes it harder to use with all kind of other macros. The [`${template}`](template.md) macro was intended to help out. This macro takes the name of macro and then treats the contents as a  `PARAMETERS`. It then applies a _template_ to each clause in the PARAMETERS. For example:

    my.plugins = ${findproviders;osgi.service;objectClass=com.example.my.Plugin}
    -runrequire.plugins = ${template;my.plugins;osgi.identity;filter:='(osgi.identity=${@})'}

This would turn the previous example into:

    -runrequire.plugins = \
        osgi.identity;filter:='(osgi.identity=com.h2database)',
        osgi.identity;filter:='(osgi.identity=org.apache.aries.async)',
        ..

        