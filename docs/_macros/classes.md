---
layout: default
class: Analyzer
title: classes ( ; QUERY ( ; PATTERN )? )*
summary: A list of class names filtered by a query language
---


The classes macro provides a query function in an analyzed bundle. While analyzing, the Analyzer stores each found class on the Bundle-Classpath with some key information. A simple query language is used to query this dictionary. For example, if you want to make a manifest header with all public classes in the bundle:

    Public-Classes: ${classes;PUBLIC}

The query language is conjunctive, that is, all entries form an AND. For example, if you want to find all PUBLIC classes that are also not abstract you would do:

    PublicConcrete-Classes: ${classes;CONCRETE}

The query can also parameters. This is a pattern that must match some aspect of the class. For example, it is possible to query for classes that extend a certain base class:

    Test-Cases: ${classes;CONCRETE;EXTENDS;junit.framework.TestCase}
    Test-Cases: ${classes;CONCRETE;ANNOTATED;org.junit.Test}

All pattern matching is based on fully qualified name and uses the globbing model.

The following table specifies what query options there are:

<table>
  <tr>
   <td><b>Query</b></td>
   <td><b>Parameter</b></td>
   <td><b>Description</b></td>
  </tr>
  <tr>
    <td>IMPLEMENTS</td>
    <td>PATTERN</td>
    <td>The class must implement at least one interface that matches the given pattern. This takes inheritance into account as long as intermediates can be found the classpath</td>
  </tr>
  <tr>
    <td>EXTENDS</td>
    <td>PATTERN</td>
    <td>The class must implement at least one interface that matches the given pattern. This takes inheritance into account as long as intermediates can be found the classpath.</td>
  </tr>
  <tr>
    <td>IMPORTS</td>
    <td>PATTERN</td>
    <td>The class must use a type from another package that matches the given pattern</td>
  </tr>
  <tr>
    <td>NAMED</td>
    <td>PATTERN</td>
    <td>The class fqn must match the given pattern.</td>
  </tr>
  <tr>
    <td>ANY</td>
    <td></td>
    <td>Matches any class</td>
  </tr>
  <tr>
    <td>VERSION</td>
    <td>PATTERN</td>
    <td>The class format of the given class must match the given version. The version is given as "major/minor", like "49/0". To select classes that are Java 6, do <code>${classes;VERSION;49/*}</code></td>
  </tr>
  <tr>
    <td>CONCRETE</td>
    <td></td>
    <td>Class must not be abstract</td>
  </tr>
  <tr>
    <td>ABSTRACT</td>
    <td></td>
    <td>Class must be abstract</td>
  </tr>
  <tr>
    <td>PUBLIC</td>
    <td></td>
    <td>Class must be public</td>
  </tr>
  <tr>
    <td>ANNOTATION</td>
    <td>PATTERN</td>
    <td>The class must be directly annotated with an annotation that matches the pattern. The set of annotations is all annotations in the class, also the annotations on fields and methods.</td>
  </tr>
  <tr>
    <td>INDIRECTLY_ANNOTATED</td>
    <td>PATTERN</td>
    <td>The class must be directly or indirectly annotated with an annotation that matches the pattern. The set of annotations is all annotations in the class, the annotations on fields and methods, and all the annotations on those annotations recursively.</td>
  </tr>
  <tr>
    <td>HIERARCHY_ANNOTATED</td>
    <td>PATTERN</td>
    <td>The class, or one of its super classes, must be directly annotated with an annotation that matches the pattern. The set of annotations is all annotations in the class, also the annotations on fields and methods.</td>
  </tr>
  <tr>
    <td>HIERARCHY_INDIRECTLY_ANNOTATED</td>
    <td>PATTERN</td>
    <td>The class, or one of its super classes, must be directly or indirectly annotated with an annotation that matches the pattern. The set of annotations is all annotations in the class, the annotations on fields and methods, and all the annotations on those annotations recursively.</td>
  </tr>
</table>


### Caveat

bnd will attempt to use the resources on the classpath if a super class or interface that is referenced from an analyzed class is not in the class space. However, bnd does not require that all dependencies are available on the classpath. In such a case it is not possible to do a complete analysis. For example, if A extends B and B extends C then it can only be determined that A extends C if B can be analyzed.
