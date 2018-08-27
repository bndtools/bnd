---
layout: default
class: Project
title: -contract 
summary: Establishes a link to a contract and handles the low level details. 
---

Though the OSGi has a very elegant package version model there are still many that think this is too much work. They do not want to be bothered by the niceties of semantic versions and just want to use, let's say, Servlet 3.0. For those people (seemingly not interested in minimizing dependencies) the OSGi Alliance came up with contracts in OSGi Core, Release 5.0.0. A contract allows you to: 

* Declare that a bundle provides the API of the given specification
* Require that the API comes from a bundle that made the declaration

This very common pattern is called the Capability/Requirement (C/R) model in OSGi, it underlies all of its dependency concepts like Import/Export package and others; it forms the foundation of the OSGi Bundle Repository. If you ever want to know what is happening deep down inside a framework than look at the Wiring API and you see the requirements and capabilities in their most fundamental form. 

Capabilities declare a set of properties that describe something that a bundle can provide. A Requirement in a bundle has a filter that must match a capability before this bundle can be resolved. To prevent requirements matching completely unrelated capabilities they must both be defined in the same namespace, where the namespace then defines the semantics of the properties. Using the C/R model we were able to describe most of the OSGi dependencies with surprisingly few additional concepts. For a modern OSGi resolver there is very little difference between the `Import-Package` and `Require-Bundle` headers.

So how do those contracts work? Well, the bundle that provides the API for the contract has a contract capability. What this means is that it provides a `Provide-Capability` clause in the `osgi.contract` namespace, for example:

```properties
# Bundle P:
Provide-Capability:\
  osgi.contract;\
    osgi.contract=Servlet;\
    uses:="javax.servlet,javax.servlet.http";\
    version="3.0"
Export-Package: javax.servlet, javax.servlet.http
```

This contract defines two properties, the contract name (by convention this is the namespace name as property key) and the version. A bundle that wants to rely on this API can add the following requirement to its manifest:

```properties
# Bundle R:
Require-Capability:\
  osgi.contract;\
    filter:="(&(osgi.contract=Servlet)(version=3.0))"
Import-Package: javax.servlet, javax.servlet.http
```

Experienced OSGi users should have cringed at these versionless packages, cringing becomes a gut-reaction at the sight of versionless packages. However, in this case it actually cannot harm. The previous example will ensure that Bundle P will be the class loader for the Bundle R for packages javax.servlet, javax.servlet.http. The magic is in the `uses:` directive, if the `Require-Capability` in bundle R is resolved to the `Provide-Capability` in bundle P then bundle R must import these packages from bundle P.

Obviously bnd has support for this (well, since today, i.e. version `osgi:biz.aQute.bndlib@2.2.0.20130806-071947` or later). First bnd can make it easier to create the `Provide-Capability` header since the involved packages are in the `Export-Package` as well as in the `Provide-Capability` headers. The do-no-repeat-yourself mantra dictated am `${exports}` macro. The `${exports}` macro is replaced by the exported packages of the bundle, for example:

```properties
# Bundle P:
Provide-Capability:\
  osgi.contract;\
    osgi.contract=Servlet;\
    uses:="${exports}";\
    version="3.0"
Export-Package: javax.servlet, javax.servlet.http
```

That said, the most extensive help you get from bnd is for requiring contracts. Providing a contract is not so cumbersome, after all you're the provider so you have all the knowledge and the interest in providing metadata. Consuming a contract is less interesting and it is much harder to get the metadata right. In a similar vein, bnd analyzes your classes to find out the dependencies to create the `Import-Package` statement, doing this by hand is really hard (as other OSGi developing environments can testify!)

So to activate the use of contracts, add the `-contract` instruction:

```properties
# bnd.bnd:
-contract: *
```

This instruction will give bnd permission to scan the build path for contracts, i.e. `Provide-Capability` clauses in the `osgi.contract` namespace. These declared contracts cause a corresponding requirement in the bundle when the bundle imports packages listed in the uses clause. In the example with Bundle R, bnd will automatically insert the `Require-Capability` header and remove any versions on the imported packages.

Sometimes the wildcard for the `-contract` instruction can be used to limit the contracts that are considered. Sometimes you want a specific contract but not others. Other times you want to skip a specific contract. The following example skips the 'Servlet' contract:

```properties
bnd.bnd:\
  -contract: !Servlet,*
```

**Note**: As of bnd 4.1.0 the default value for the `-contract` instruction will be `*` which result in the automatic application of any contracts found at build time.

The tests provide some examples for people that want to have a deeper understanding: https://github.com/bndtools/bnd/blob/next/biz.aQute.bndlib.tests/src/test/ContractTest.java 

## Further Reading

See also [Portable Contract Definitions](https://www.osgi.org/portable-java-contract-definitions/)


