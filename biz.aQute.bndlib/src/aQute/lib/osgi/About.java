package aQute.lib.osgi;

import aQute.libg.header.*;

/**
 * This package contains a number of classes that assists by analyzing JARs and
 * constructing bundles.
 * 
 * The Analyzer class can be used to analyze an existing bundle and can create a
 * manifest specification from proposed (wildcard) Export-Package,
 * Bundle-Includes, and Import-Package headers.
 * 
 * The Builder class can use the headers to construct a JAR from the classpath.
 * 
 * The Verifier class can take an existing JAR and verify that all headers are
 * correctly set. It will verify the syntax of the headers, match it against the
 * proper contents, and verify imports and exports.
 * 
 * A number of utility classes are available.
 * 
 * Jar, provides an abstraction of a Jar file. It has constructors for creating
 * a Jar from a stream, a directory, or a jar file. A Jar, keeps a collection
 * Resource's. There are Resource implementations for File, from ZipFile, or
 * from a stream (which copies the data). The Jar tries to minimize the work
 * during build up so that it is cheap to use. The Resource's can be used to
 * iterate over the names and later read the resources when needed.
 * 
 * Clazz, provides a parser for the class files. This will be used to define the
 * imports and exports.
 * 
 * Headers are translated to {@link Parameters} that contains all headers (the
 * order is maintained). The attribute of each header are maintained in an
 * {@link Attrs}. Each additional file in a header definition will have its own
 * entry (only native code does not work this way). The ':' of directives is
 * considered part of the name. This allows attributes and directives to be
 * maintained in the Attributes map.
 * 
 * An important aspect of the specification is to allow the use of wildcards.
 * Wildcards select from a set and can decorate the entries with new attributes.
 * This functionality is implemented in Instructions.
 * 
 * Much of the information calculated is in packages. A package is identified
 * by a PackageRef (and a type by a TypeRef). The namespace is maintained
 * by {@link Descriptors}, which here is owned by {@link Analyzer}. A special
 * class, {@link Packages} maintains the attributes that are found in the code.
 * 
 * @version $Revision: 1.2 $
 */
public class About {

}
