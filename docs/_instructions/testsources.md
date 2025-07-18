---
layout: default
title: -testsources REGEX ( ',' REGEX )*
class: Project
summary: |
   Specification to find JUnit test cases by traversing the test src directory and looking for java classes. The default is (.*).java.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-testsources=*.java`

- Values: `REGEX ( ',' REGEX )*`

- Pattern: `.*`

<!-- Manual content from: ext/testsources.md --><br /><br />

The `-testsources` instruction specifies how bnd should find JUnit test cases by searching the test source directory for Java classes. By default, it looks for all `.java` files, but you can provide a regular expression to customize which files are considered test sources.

This instruction is useful for projects with non-standard test file naming or organization, allowing you to control which files are included as test cases during the build process.
