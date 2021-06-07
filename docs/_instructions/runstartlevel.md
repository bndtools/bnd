---
layout: default
class: Project
title: -runstartlevel ( order | begin | step )*
summary:  Assign a start level to each run-bundle after resolving 
---

After a [resolve][1] the resolver calculates a number of resources that are mapped to bundles. This mapping can
include ordering and assigned startlevels. The basic instruction that parameterizes this is `-runstartlevel`. 

## Default behavior when not set

If `-runstartlevel` is not set the set of -runbundles will be sorted by name and version after which it is merged with the existing
`-runbundles`. Setting the `-runstartlevel` makes it possible to let bnd assign startlevels based on different
ordering strategies.

## Syntax

This instruction has the following syntax:

    -runstartlevel      ::= runstartlevel ( ',' runstartlevel )
    runstartlevel       ::  order | begin | step
    order               ::= 'order=' ORDER
    ORDER               ::= 
            'leastdependenciesfirst' 
        |   'leastdependencieslast'
        |   'random'
        |   'sortbynameversion'
        |   'mergesortbynameversion'
    begin               ::= 'begin=' NUMBER
    step                ::= 'step=' NUMBER

The final `-runstartlevel` flattens the properties so that the last of each of `order`, `begin`, or `step` will be used.

### Order Types

The value of `order` can take on the following values:

* `mergesortbynameversion` – Ordering by name (and version) and then merging was the original behavior. This is therefore the default.
* `sortbynameversion` – For completeness, this option orders the bundles by name and version and then assigns a startlevel.
* `random` – Use a random ordering. The ordering uses an algorithm that is based on the random number generator and should therefore 
  be different on each run.
* `leastdependenciesfirst` – Sort the resources _topologically_ and place the resources with the least dependencies first.
* `leastdependencieslast` – Sort the resources _topologically_ and place the resources with the least dependencies last.

The topological sorting algorithm is based on [Tarjan][2]. It can handle cyclic dependencies well. However, cyclic dependencies
make the ordering not perfect.

## Startlevels

After the resources have been resolved they are sorted according to the `order`. If the `begin` attribute is set and
higher than 0, the resources will be assigned a startlevel that starts at the given value. By default, the step for each
bundle is 10. (A lesson taught to us by BASIC) The step can be overridden by setting the `step` value.

If the `begin` value is not set, or it is set to a value < 1, then no startlevel attribute is added. However, the
order of the `-runbundles` will be in the specified `order`. In most cases, this is then some kind of _natural_ ordering
since launchers start these 

If you set the `step` to 0 then all bundles will be assigned the same startlevel. However, the `-runbundles` has the proper 
order. 

## Example

    -runstartlevel: \
        order = leastdependenciesfirst, \
        begin = 1000, \
        step  =    1

After resolving, this can generate:

	-runbundles: \
		org.apache.felix.configadmin;version='[1.8.8,1.8.9)';startlevel=1000,\
		org.apache.felix.http.jetty;version='[3.2.0,3.2.1);startlevel=1001',\
		org.apache.felix.http.servlet-api;version='[1.1.2,1.1.3);startlevel=1002',\
		...
		osgi.enroute.twitter.bootstrap.webresource;version='[3.3.5,3.3.6);startlevel=1019',\
		osgi.enroute.web.simple.provider;version='[2.1.0,2.1.1);startlevel=1020'

[1]: /chapters/250-resolving.html
[2]: https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
