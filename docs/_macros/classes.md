---
layout: default
class: Analyzer
title: classes TODO
summary: A list of class names filtered by a query language
---


The classes macro provides a query function in an analyzed bundle. While analyzing, the Analyzer stores each found class on the Bundle-Classpath with some key information. A simple query language is used to query this dictionary. For example, if you want to make a manifest header with all public classes in the bundle:

  	Public-Classes: ${classes;PUBLIC}

The query language is conjunctive, that is, all entries form an AND. For example, if you want to find all PUBLIC classes that are also not abstract you would do:

  PublicConcrete-Classes: ${classes;CONCRETE}

The query can also parameters. This is a pattern that must match some aspect of the class. For example, it is possible to query for classes that extend a certain base class:

  Test-Cases: ${classes;CONCRETE;EXTENDS;junit.framework.TestCase}

All pattern matching is based on fully qualified name and uses the globbing model.

The following table specifies what query options there are:

<table>
'''Query'''
<td>'''Parameter'''</td>
<td>'''Description'''</td>

IMPLEMENTS
<td>PATTERN</td>
<td>The class must implement at least one interface that matches the given pattern. This takes inheritance into account as long as intermediates can be found the classpath</td>

EXTENDS
<td>PATTERN</td>
<td>The class must implement at least one interface that matches the given pattern. This takes inheritance into account as long as intermediates can be found the classpath.</td>

IMPORTS
<td>PATTERN</td>
<td>The class must use a type from another package that matches the given pattern</td>

NAMED
<td>PATTERN</td>
<td>The class fqn must match the given pattern.</td>

ANY
<td></td>
<td>Matches any class</td>

VERSION
<td>PATTERN</td>
<td>The class format of the given class must match the given version. The version is given as "<major>/<minor>", like "49/0". To select classes that are Java 6, do `${classes;VERSION;49/*}`</td>

CONCRETE
<td></td>
<td>Class must not be abstract</td>

ABSTRACT
<td></td>
<td>Class must be abstract</td>

PUBLIC
<td></td>
<td>Class must be public</td>

ANNOTATION
<td>PATTERN</td>
<td>The class must be directly annotated with an annotation that matches the pattern. The set of annotations is all annotations in the class, also the annotations on fields and methods.</td>

INDIRECTLY_ANNOTATED
<td>PATTERN</td>
<td>The class must be directly or indirectly annotated with an annotation that matches the pattern. The set of annotations is all annotations in the class, the annotations on fields and methods, and all the annotations on those annotations recursively.</td>
(:tableend:)

###Caveat
bnd will attempt to use the resources on the classpath if a super class or interface that is referenced from an analyzed class is not in the class space. However, bnd does not require that all dependencies are available on the classpath. In such a case it is not possible to do a complete analysis. For example, if A extends B and B extends C then it can only be determined that A extends C if B can be analyzed.
 
 

	static String	_classesHelp	= "${classes;'implementing'|'extending'|'importing'|'named'|'version'|'any';<pattern>}, Return a list of class fully qualified class names that extend/implement/import any of the contained classes matching the pattern\n";

	public String _classes(String... args) throws Exception {
		// Macro.verifyCommand(args, _classesHelp, new
		// Pattern[]{null,Pattern.compile("(implementing|implements|extending|extends|importing|imports|any)"),
		// null}, 3,3);

		Collection<Clazz> matched = getClasses(args);
		if (matched.isEmpty())
			return "";

		return join(matched);
	}
