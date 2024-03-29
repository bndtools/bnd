---
order: 390
title: Wrapping Libraries to OSGi Bundles
summary: Discusses wrapping libraries into bundles
layout: default
author: Neil Bartlett (edited/updated by Peter Kriens)
---

OSGi developers face a challenge when using third-party libraries that
are not supplied as OSGi bundles. Though an increasing number of
libraries are available from their original sources as OSGi bundles, and
a large number are available as wrapped OSGi bundles from external
repositories, it is still sometimes necessary to build such a wrapper
ourselves. This technical note details an approach to OSGi bundle
production using only command line tools.

## Introduction

This article details a simple and repeatable process to wrap
arbitrary Java libraries as OSGi bundles, using bnd as a command line
tool.

As a running example, the [JDOM library][] version [1.1.2][] will be wrapped as
an OSGi bundle.

NB: Many of the tasks described here can be more easily performed with a
full-featured OSGi IDE such as Bndtools. However, this document is
intended for users who perform these tasks infrequently and do not wish
to download an IDE; instead a single, lightweight
command-line tool is used. 

Bnd generates the `Import-Package` statement of the output bundle via an
extremely thorough inspection of the compiled Java. Every bytecode
instruction of every class file inside the bundle is processed to
discover dependencies on external packages. Usually the result of this
inspection is more accurate than we would be able to achieve by manually
providing the `Import-Package` statement.

Unfortunately when wrapping third-party libraries it is sometimes not
sufficient to simply accept the generated `Import-Package` statement:
the result may need to be fine-tuned. This is because many third-party
libraries contain dependencies that are out of place, often due to
errors resulting from a lack of good modular practices.

For example:

-   Classes that implement optional features are sometimes placed into a
    library’s "core" JAR. For example the Log4J library includes
    optional "appenders" for writing log messages to emails, JMS queues
    and JMX/JDMK. As a result it depends on inter alia the javax.jms
    package, and we have to include the JMS API bundle in order for
    logging to work at all!
-   In other cases a library may contain "dead code" — i.e. code that is
    not reachable from the public API — and that code may have external
    dependencies.

Bnd detects dependencies statically by inspecting all code in the
library; it cannot determine which parts of the library are reachable.
For example a common error is to include JUnit test cases in a library
JAR, resulting in dependencies on JUnit. Unless fixed, the bundle will
only be usable in a runtime environment where JUnit is also present,
i.e., we will have to ship a copy of JUnit to our end users.

The problem of checking for and correcting such problems represents the
bulk of the manual effort required in what is otherwise a fairly
automatic process.

## Initial Wrapping

We assume that the [JDOM library][] has been downloaded, and `jdom.jar` is
available in the current directory.

In order to wrap as a bundle using bnd, we need an initial "recipe".
Create a file named `jdom.bnd` containing the following:

    -classpath: jdom.jar
    Bundle-SymbolicName: org.jdom
    ver: 1.1.2
    -output: ${bsn}-${ver}.jar
    Bundle-Version: ${ver}
    Export-Package: *;version=${ver}

This is a bnd descriptor, and it instructs bnd how to generate the OSGi
bundle. To summarize the features used:

-   Line 1 indicates the JAR to be processed. If desired, we can merge
    multiple original JAR files into a single bundle.
-   Line 2 indicates the Bundle-SymbolicName (BSN) of the output bundle.
    This should follow Java package name conventions.
-   Line 3 declares an internal value named ver that contains the
    version of the JDOM API. This value is referenced elsewhere in the
    descriptor.
-   Line 5 specifies the file name of the output bundle. The BSN and
    version properties are referenced via macros.
-   Line 6 specifies the OSGi bundle version, using the version value
    declared on line 3.
-   Line 7 indicates that all packages found in the input JAR (i.e.
    "\*") should be declared as exports of the bundle, and additionally
    these exports are marked with the version of the API.

To generate the bundle: bnd reports the name of the generated file
(org.jdom-1.1.2.jar), the number of files contained (79) and its size in
bytes (151K). We refer to this bundle as the initial wrapping.

## Examining Dependencies

The intial wrapping may contain dependency errors as described in the
introduction. Therefore we must examine the `Import-Package` statement
as generated by bnd. Unfortunately, direct viewing of the `MANIFEST.MF`
can be difficult due to the unusual formatting and line-wrapping rules
of the manifest file format that make it quite inaccessible. For
example:

    $ bnd jdom.bnd
    org.jdom-1.1.2.jar 79 154490
    Import-Package: javax.xml.parsers,javax.xml.transform,javax.xml.transf
     orm.sax,javax.xml.transform.stream,oracle.xml.parser,oracle.xml.parse
     r.v2,org.apache.xerces.dom,org.apache.xerces.parsers,org.jaxen,org.ja
     xen.jdom,org.jdom;version="[1.1,2)",org.jdom.adapters;version="[1.1,2
     )",org.jdom.filter;version="[1.1,2)",org.jdom.input;version="[1.1,2)"
     ,org.jdom.output;version="[1.1,2)",org.jdom.transform;version="[1.1,2
     )",org.jdom.xpath;version="[1.1,2)",org.w3c.dom,org.xml.sax,org.xml.s
     ax.ext,org.xml.sax.helpers

Since this is so unreadable, Bnd offers a print command that formats in
the manifest of a specified bundle JAR. We can request Bnd to print only
the imports and exports by using the `-impexp` switch: 

    $ bnd print -impexp org.jdom-1.1.2.jar
    [IMPEXP]
    Import-Package
      javax.xml.parsers
      javax.xml.transform
      javax.xml.transform.sax
      javax.xml.transform.stream
      oracle.xml.parser
      oracle.xml.parser.v2
      org.apache.xerces.dom
      org.apache.xerces.parsers
      org.jaxen
      org.jaxen.jdom
      org.jdom                    {version=[1.1,2)}
      org.jdom.input              {version=[1.1,2)}
      org.w3c.dom
      org.xml.sax
      org.xml.sax.ext
      org.xml.sax.helpers
    Export-Package
      org.jdom                    {version=1.1.2, imported-as=[1.1,2)}
      org.jdom.adapters           {version=1.1.2}
      org.jdom.filter             {version=1.1.2}
      org.jdom.input              {version=1.1.2, imported-as=[1.1,2)}
      org.jdom.output             {version=1.1.2}
      org.jdom.transform          {version=1.1.2}
      org.jdom.xpath              {version=1.1.2}

Reviewing the imports, we see that most of them come
from JRE packages. However there are three groups of dependencies that
may cause problems: the Oracle XML parser; the Xerces XML parser; and
the Jaxen XPath library. Unless something is done to fix these, our JDOM
bundle will not work unless all three dependencies are present at
runtime.

## Refining Dependencies

First we consider the Oracle XML parser dependency. We anticipate that
this probably derives from an optional feature, and can therefore be
considered an optional dependency. Importing a package with the
`resolution:=optional` directive allows our bundle to see the specified
package at runtime if an exported for it is available, but it does not
prevent the bundle from resolving in the event that an exported is not
available.

### Indicating Optional Dependencies

To mark the two Oracle imports as optional, add the following line to
the `jdom.bnd` file:

    Import-Package: \
    	oracle.xml.*;resolution:=optional, \
    	*

If we regenerate the bundle and again ask Bnd to print the imports and
exports, we will see that the Oracle dependencies are marked as
optional;

    [IMPEXP]
    Import-Package
      ...
      oracle.xml.parser          {resolution:=optional}
      oracle.xml.parser.v2       {resolution:=optional}
      ...

We can do the same for the Xerces packages:

    Import-Package: \
    	oracle.xml.*;resolution:=optional,\
		org.apache.xerces.*;resolution:=optional,\
		*

**NB:** the final **\*** in the `Import-Package` statements is
**extremely important**. Without this, all other dependencies detected
by Bnd will be omitted from the final manifest.

### Package Level Used-By Analysis

In some cases the root cause of a dependency is unclear, or we may wish
to obtain further information on the causes of a dependency.

JDOM depends on Jaxen, an XPath evaluation library. However not all
use-cases for JDOM involve evaluating XPath expressions, so this may be
an optional dependency. To get further information to help us make this
decision, we can use the Bnd `print` command again with the `-usedby`
option: 

    $ bnd print -usedby org.jdom-1.1.2.jar
    [USEDBY]
    java.sql                   org.jdom
    javax.xml.parsers          org.jdom.adapters
    ...
    org.jaxen                  org.jdom.xpath
    org.jaxen.jdom             org.jdom.xpath
    org.jdom
                                          . 
                               org.jdom.adapters
                               org.jdom.filter
    ...

This tells us that the Jaxen dependencies (i.e. the
org.jaxen and org.jaxen.jdom packages) are used only from one package in
JDOM, namely org.jdom.xpath. Additionally by looking at the full results
we can see that org.jdom.xpath does not appear on the left hand side,
meaning that it is not imported by any other part of the JDOM library.

### Splitting the Bundle

If we simply make our Jaxen imports optional, then a client that imports
`org.jdom.xpath` from the JDOM bundle will get `NoClassDefFoundError` or
`ClassNotFoundException` when it tries to use the XPath features. In
this case it is better to separate `org.jdom.xpath` into a new bundle.
Once separated, any client that explicitly needs the XPath features will
not resolve when the Jaxen bundle offering those features is
unavailable, which is the desired outcome: it is better to get a
resolution error than a runtime exception. Separation works in this case
because the `org.jdom.xpath` package has good coherency (i.e., it does
just one thing) and there are no references to it from elsewhere in
JDOM. If there were such references then the two bundles would be
tightly coupled to each other and the separation would be pointless.

In order to separate the bundles, we first need to omit `org.jdom.xpath`
from the exports of our main JDOM bundle. This is done by refining the
`Export-Package` statement as follows:

    Export-Package: !org.jdom.xpath,\ *;version=${ver}

The leading exclamation mark can be read as "not" and it simply excludes
the named package from the generated bundle. Alternative we can just
list each package explicitly, though this requires us to repeat the
version directive on each line:

    Export-Package: org.jdom;version=${ver},\
      org.jdom.adapters;version=${ver},\
      org.jdom.filter;version=${ver},\
      org.jdom.input;version=${ver},\
      org.jdom.output;version=${ver},\
      org.jdom.transform;version=${ver}

We will also need a Bnd descriptor named jdom.xpath.bnd to generate the
JDOM XPath bundle. This is based on our original recipe:

    -classpath: jdom.jar
    Bundle-SymbolicName: org.jdom.xpath
    ver: 1.1.2
    -output: ${bsn}-${ver}.jar
    Bundle-Version: ${ver}
    Export-Package: org.jdom.xpath;version=${ver}



### Class Level Used-By Analysis

The previous used-by analysis yielded the package from which an import
dependency resulted. Sometimes we need to dig deeper and find the
individual class(es) responsible for the dependency. Unfortunately this
feature is not available from the Bnd command line, instead we have to
use a macro inside the descriptor file. To find out the class-level
causes for the dependency on the oracle.xml.parser package, add the
following temporary header to the descriptor:

`TEMP: ${classes;IMPORTING;oracle.xml.parser}`

This will result in a TEMP header being added to the manifest of the
output bundle. To view it, use Bnd’s print command again with the
-manifest option:

    $ bnd print -manifest org.jdom-1.1.2.jar
    [MANIFEST org.jdom-1.1.2.jar]
    ...
    TEMP                                    org.jdom.adapters.OracleV1DOMAdapter
    ...

The output shows that the Oracle parser is used only from a single
class, strongly supporting our opinion that it is an optional
dependency. Unfortunately we cannot separate it into its own bundle
because there are several other such adapters in the same package, each
with their own dependencies. Performing the same analysis on the Oracle
parser V2 and Xerces dependencies yields similar results: individual
classes in the `org.jdom.adapters` package. This is poor modular design
on the part of JDOM, but we cannot fix it without breaking existing
clients. Therefore marking the dependencies as optional is the best
solution.

Even deeper analysis is sometimes required: e.g., do any other classes
in JDOM refer to `OracleV1DOMAdapter`? Such questions are best answered
by searching the source code of the library. When source code is not
available then disassembly with the javap tool or decompilation with JAD
usually helps.

After performing class level used-by analysis, remember to remove any
temporary headers, i.e. TEMP.

### Excluding Imports

Undesirable imports can also result from "dead code", i.e. code that is
not reachable from any part of the public API of the library. For
example libraries sometimes contain JUnit test cases or old classes that
are no longer used. JDOM does not suffer from such problems, but let us
suppose for a moment that it did contain an invalid dependency on JUnit.
We could completely remove the imported `org.junit` package by adding an
exclusion rule to `Import-Package`:

    Import-Package: \
    	oracle.xml.parser*;resolution:=optional,\
		org.apache.xerces.*;resolution:=optional,\
    	!org.junit*,\ 
    	*

Exclusion rules should be used with caution, as they can cause the
bundle to produce NoClassDefFoundError. Careful used-by analysis should
be performed to ensure that the dependency really is only relevant to
unreachable code.

### Versioning Imports

In this example bnd file, we do not provide the dependency JARs, bnd therefore does not
have the ability to automatically calculate the correct version of an
import because the target package is usually not visible when
the bundle is created. Therefore bnd generates imports that use the
default import version range, which in OSGi is implicitly "[0, ∞)", or
"zero to infinity" —in other words, a range that matches any version.

When such a wide version range is used, the bundle will normally work
initially, but will suffer problems when the API of the imported package
evolves. For example, a major new version of the imported API may
introduce breaking changes, but our bundle will still resolve against
it. This could result in errors such as `IncompatibleClassChangeError`,
`AbstractMethodError`, `NoSuchMethodError`, etc. Therefore we should
manually add import ranges where possible.

For example the JDOM version we are wrapping has been built against
version 1.1.2. The Jaxen imports can be refined by adding the following
`Import-Package` statement to `jdom.xpath.bnd`:

    Import-Package: \
        org.jaxen.*;version="[1.1,2)",\
        *

Note the import range 1.1 through 2, exclusive of 2 — this is in
compliance with OSGi [Semantic Versioning][] guidelines. The
API library may not follow the OSGi guidelines so sometimes an
alternative range may be required.

Often our biggest problem is working out which version of a dependency
library was used to build the library we are wrapping. In the case of
JDOM, we can find jaxen.jar in the lib directory of its source project
and note that its manifest indicates version 1.1.2. If the project is
built with Apache Maven we can usually find a version in the POM. Other
times we must resort to reading project documentation, if it exists.

Note that version ranges cannot be added for JRE packages, e.g.
`javax.swing` or `org.xml.sax` because the Java specifications do not
define the version of any of these packages, and therefore the OSGi
framework exports them all as version 0.0.0. As an alternative, add a
`Bundle-RequiredExecutionEnvironment` header to indicate the basic Java
level required by the bundle:

    Bundle-RequiredExecutionEnvironment: J2SE-1.5

Other possible values include JavaSE-1.6, OSGi/Minimum-1.0, etc.

Other Concerns
--------------

### Class References in Configuration

Bnd discovers package dependencies in a bundle by scanning the bytecode
of the compiled Java files. This process finds all of the static
dependencies of the Java code, but it does not discover dynamic
dependencies, for example those arising from the use of
`Class.forName()`. There is no generic way for Bnd to calculate all
dynamic dependencies. However there are certain well-known configuration
formats that result in dynamic dependencies, and Bnd can analyse these
formats through the use of plugins.

For example, some bundles use the Spring Framework for dependency
injection. Spring uses XML files that refer to fully qualified Java
class names:

    <bean id="myBean" class="org.example.beans.MyBean">
    </bean>

Here the `org.example.beans` package is a dependency of the bundle that
should be added to `Import-Package`. Bnd can discover this dependency by
adding a Spring analyser plugin via a declaration in the descriptor
file:

    -plugin: aQute.lib.spring.SpringComponent

Similar plugins exist for JPA and Hibernate, and custom plugins can be
written to support other configuration formats or scripting languages.

Summary
-------

In summary the process of wrapping a JAR as an OSGi bundle is as
follows:

1. Create the bnd descriptor, using the template in Appendix A.

2. Generate the initial wrapping and review the imports for suspicious
dependencies.

3. Fix problematic dependencies using the following heuristics:

-   if the dependency arises from a package that is coherent, has an
    essential need for the dependency, and is not referred from other
    parts of the bundle, separate this package into its own bundle;
-   if the dependency arises from a small number of classes that are not
    referred from other parts of the bundle, mark the dependency as
    optional;
-   if the dependency arises from unreachable "dead code", exclude the
    dependency;
-   otherwise the dependency is probably valid and necessary, so should
    be left in the bundle.

4. Add import version ranges for non-JRE imports, where possible.


## A Template

The following template bnd descriptor can be used for the initial
wrapping. The placeholders on the first three lines must be filled in:

    -classpath: <INPUT JAR(S)>
    Bundle-SymbolicName: <NAME>
    ver: <VERSION>
    -output: ${bsn}-${ver}.jar
    Bundle-Version: ${ver}
    Export-Package: *;version=${ver}
    # Uncomment next line to customize imports. The last entry MUST be "*"
    # Import-Package: *

[JDOM library]: https://search.maven.org/search?q=g:org.jdom%20AND%20a:jdom&core=gav
[1.1.2]: https://search.maven.org/artifact/org.jdom/jdom/1.1.2/jar
[Semantic Versioning]: https://docs.osgi.org/whitepaper/semantic-versioning/

