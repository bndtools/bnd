---
layout: default
class: Analyzer
title: classes ( ; QUERY ( ; PATTERN )? )*
summary: A list of class names filtered by a query language
---


The classes macro provides a query function in an analyzed bundle. While analyzing, the Analyzer stores each found class on the Bundle-Classpath with some key information. A simple query language is used to query this dictionary. For example, if you want to make a manifest header with all public classes in the bundle:

    Public-Classes: ${classes;PUBLIC}

The query language is conjunctive, that is, all entries form an AND. For example, if you want to find all PUBLIC classes that are also not abstract you would do:

    PublicConcrete-Classes: ${classes;PUBLIC;CONCRETE}

Some query types can also take parameters. This is a pattern that must match some aspect of the class. For example, it is possible to query for classes that extend a certain base class or is annotated by a certain annotation type:

    Test-Cases: ${classes;CONCRETE;EXTENDS;junit.framework.TestCase}
    Test-Cases: ${classes;CONCRETE;HIERARCHY_ANNOTATED;org.junit.Test}
    Test-Cases: ${classes;CONCRETE;HIERARCHY_INDIRECTLY_ANNOTATED;org.junit.platform.commons.annotation.Testable}

All pattern matching is based on fully qualified name and uses the globbing model.

The following table specifies what query options there are:

<table>
  <tr>
   <th><b>Query</b></th>
   <th><b>Parameter</b></th>
   <th><b>Description</b></th>
  </tr>
  <tr>
    <td>ANY</td>
    <td></td>
    <td>Matches any class</td>
  </tr>
  <tr>
    <td>CONCRETE</td>
    <td></td>
    <td>Class must not be abstract.</td>
  </tr>
  <tr>
    <td>ABSTRACT</td>
    <td></td>
    <td>Class must be abstract.</td>
  </tr>
  <tr>
    <td>PUBLIC</td>
    <td></td>
    <td>Class must be public.</td>
  </tr>
  <tr>
    <td>STATIC</td>
    <td></td>
    <td>Class must be explicitly or implicitly declared static. That is, the class must not be a inner class.</td>
  </tr>
  <tr>
    <td>INNER</td>
    <td></td>
    <td>Class must be an inner class. That is, the class must be a nested class that is not explicitly or implicitly declared static. Inner classes include anonymous and local classes.</td>
  </tr>
  <tr>
    <td>CLASSANNOTATIONS</td>
    <td></td>
    <td>The class must have some CLASS retention annotations.</td>
  </tr>
  <tr>
    <td>RUNTIMEANNOTATIONS</td>
    <td></td>
    <td>The class must have some RUNTIME retention annotations.</td>
  </tr>
  <tr>
    <td>DEFAULT_CONSTRUCTOR</td>
    <td></td>
    <td>The class must have a default constructor. That is, the class must have a public, no-argument constructor.</td>
  </tr>
  <tr>
    <td>IMPLEMENTS</td>
    <td>PATTERN</td>
    <td>The class must implement at least one fully qualified interface name that matches the given pattern. This takes inheritance into account as long as super types can be found on the classpath.  This query uses the Java source code fully qualified name where `.` (not `$`) separates the nested class name from the outer class name.</td>
  </tr>
  <tr>
    <td>EXTENDS</td>
    <td>PATTERN</td>
    <td>The class must extend at least one fully qualified class name that matches the given pattern. This takes inheritance into account as long as super types can be found on the classpath. This query uses the Java source code fully qualified name where `.` (not `$`) separates the nested class name from the outer class name.</td>
  </tr>
  <tr>
    <td>IMPORTS</td>
    <td>PATTERN</td>
    <td>The class must use a type from another package name that matches the given pattern</td>
  </tr>
  <tr>
    <td>NAMED</td>
    <td>PATTERN</td>
    <td>The fully qualified name of the class must match the given pattern. This query uses the Java source code fully qualified name where `.` (not `$`) separates the nested class name from the outer class name.</td>
  </tr>
  <tr>
    <td>VERSION</td>
    <td>PATTERN</td>
    <td>The class format of the given class must match the given version. The version is given as "major.minor", like "49.0". To select classes that are Java 6, do <code>${classes;VERSION;49.*}</code></td>
  </tr>
  <tr>
    <td>ANNOTATED</td>
    <td>PATTERN</td>
    <td>The class must be directly annotated with a fully qualified annotation name that matches the pattern. The set of annotations is all annotations in the class, also the annotations on fields and methods. This query uses the Java class name fully qualified name where `$` (not `.`) separates the nested class name from the outer class name.</td>
  </tr>
  <tr>
    <td>INDIRECTLY_ANNOTATED</td>
    <td>PATTERN</td>
    <td>The class must be directly or indirectly annotated with a fully qualified annotation name that matches the pattern. The set of annotations is all annotations in the class, the annotations on fields and methods, and all the annotations on those annotations recursively. This query uses the Java class name fully qualified name where `$` (not `.`) separates the nested class name from the outer class name.</td>
  </tr>
  <tr>
    <td>HIERARCHY_ANNOTATED</td>
    <td>PATTERN</td>
    <td>The class, or one of its super classes, must be directly annotated with a fully qualified annotation name that matches the pattern. The set of annotations is all annotations in the class, also the annotations on fields and methods. This query uses the Java class name fully qualified name where `$` (not `.`) separates the nested class name from the outer class name.</td>
  </tr>
  <tr>
    <td>HIERARCHY_INDIRECTLY_ANNOTATED</td>
    <td>PATTERN</td>
    <td>The class, or one of its super classes, must be directly or indirectly annotated with a fully qualified annotation name that matches the pattern. The set of annotations is all annotations in the class, the annotations on fields and methods, and all the annotations on those annotations recursively. This query uses the Java class name fully qualified name where `$` (not `.`) separates the nested class name from the outer class name.</td>
  </tr>
</table>


### Caveat

bnd will attempt to use the resources on the classpath if a super class or interface that is referenced from an analyzed class is not in the class space. However, bnd does not require that all dependencies are available on the classpath. In such a case it is not possible to do a complete analysis. For example, if A extends B and B extends C then it can only be determined that A extends C if B can be analyzed.
